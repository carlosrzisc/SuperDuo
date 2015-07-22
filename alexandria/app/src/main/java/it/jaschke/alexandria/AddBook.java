package it.jaschke.alexandria;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v7.widget.SearchView;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;
import it.jaschke.alexandria.services.DownloadImage;


public class AddBook extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private SearchView ean;
    private View rootView;
    private final String EAN_CONTENT="eanContent";
    private static final String SCAN_FORMAT = "SCAN_RESULT_FORMAT";

    private static final String SCAN_RESULT = "SCAN_RESULT";
    private static final int SCAN_REQUEST_CODE = 49374;
    private static final String SCAN_FORMAT_QRCODE = "QR_CODE";
    private static final String SCAN_FORMAT_BARCODE = "EAN_13";

    private String currentISBN;

    FragmentIntentIntegrator scanner;

    public AddBook(){
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(ean != null) {
            outState.putString(EAN_CONTENT, ean.getQuery().toString());
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_add_book, container, false);
        ean = (SearchView) rootView.findViewById(R.id.ean);
        ean.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String eanString = query;
                //catch isbn10 numbers
                if (eanString.length() == 10 && !eanString.startsWith("978")) {
                    eanString = "978" + eanString;
                    ean.setQuery(eanString, false);
                }
                if (eanString.length() < 13 || eanString.length() > 13) {
                    // Need to validate greater than 13 too because searchview does not support maxLength
                    Toast.makeText(getActivity(), getString(R.string.input_hint), Toast.LENGTH_SHORT).show();
                    clearFields();
                    return true;
                }
                currentISBN = eanString;
                //Once we have an ISBN, start a book intent
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, eanString);
                bookIntent.setAction(BookService.FETCH_BOOK);
                getActivity().startService(bookIntent);
                AddBook.this.restartLoader();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() == 0) {
                    // workaround for OnCloseListener not working on searchview when property
                    // iconifiedByDefault = false
                    clearFields();
                }
                return false;
            }
        });
        ean.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                ean.setQuery("", true);
                return false;
            }
        });
        scanner = new FragmentIntentIntegrator(this);
        rootView.findViewById(R.id.scan_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanner.initiateScan();
            }
        });

        rootView.findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ean.setQuery("", false);
                clearFields();
            }
        });

        rootView.findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent bookIntent = new Intent(getActivity(), BookService.class);

                // Following line is a defect because sending the ean input text value, that does
                // not ensure this was the book just added, it can cause not desired behavior since
                // user can change the ean input text just after it was added, it can even cause
                // a NumberFormatException, here we are going to use the ISBN used to add the
                // current book, and I will add a try catch for the NumberFExc on the Service
//                bookIntent.putExtra(BookService.EAN, ean.getQuery().toString());
                bookIntent.putExtra(BookService.EAN, currentISBN);

                bookIntent.setAction(BookService.DELETE_BOOK);
                getActivity().startService(bookIntent);
                ean.setQuery("", false);
                clearFields();
            }
        });

        if (savedInstanceState != null) {
            ean.setQuery(savedInstanceState.getString(EAN_CONTENT), true);
        }

        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String scanFormat = data.getStringExtra(SCAN_FORMAT);
            if (requestCode == SCAN_REQUEST_CODE && null != scanFormat) {
                String isbnCode = "";
                if (scanFormat.equalsIgnoreCase(SCAN_FORMAT_QRCODE)) {
                    isbnCode = Utility.getISBNNumberFromQRCode(data.getStringExtra(SCAN_RESULT));
                } else if (scanFormat.equalsIgnoreCase(SCAN_FORMAT_BARCODE)) {
                    isbnCode = data.getStringExtra(SCAN_RESULT);
                }
                if (!isbnCode.isEmpty()) {
                    if (ean != null) {
                        ean.setQuery(isbnCode, true);
                    }
                } else {
                    Toast.makeText(
                            getActivity(),
                            getString(R.string.error_isbn_not_found),
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void restartLoader(){
        int LOADER_ID = 1;
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if(ean.getQuery().length()==0){
            return null;
        }
        String eanStr= ean.getQuery().toString();
        if(eanStr.length()==10 && !eanStr.startsWith("978")){
            eanStr="978"+eanStr;
        }
        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(eanStr)),
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) {
            return;
        }

        String bookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        ((TextView) rootView.findViewById(R.id.bookTitle)).setText(bookTitle);

        String bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText(bookSubTitle);

        String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));
        if (authors == null) {
            authors = "";
        }
        String[] authorsArr = authors.split(",");
        ((TextView) rootView.findViewById(R.id.authors)).setLines(authorsArr.length);
        ((TextView) rootView.findViewById(R.id.authors)).setText(authors.replace(",","\n"));
        String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));
        if(Patterns.WEB_URL.matcher(imgUrl).matches()){
            new DownloadImage((ImageView) rootView.findViewById(R.id.bookCover)).execute(imgUrl);
            rootView.findViewById(R.id.bookCover).setVisibility(View.VISIBLE);
        }

        String categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));
        ((TextView) rootView.findViewById(R.id.categories)).setText(categories);

        rootView.findViewById(R.id.save_button).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.delete_button).setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    private void clearFields(){
        ((TextView) rootView.findViewById(R.id.bookTitle)).setText("");
        ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText("");
        ((TextView) rootView.findViewById(R.id.authors)).setText("");
        ((TextView) rootView.findViewById(R.id.categories)).setText("");
        rootView.findViewById(R.id.bookCover).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.save_button).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.delete_button).setVisibility(View.INVISIBLE);
        currentISBN = "";
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.scan);
    }
}
