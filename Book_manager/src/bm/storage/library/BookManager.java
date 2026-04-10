package bm.storage.library;

import java.util.ArrayList;
import java.util.List;

import bm.model.book.Book;
import kayanko.util.parameter.validation.ParamCheck;

public class BookManager {

	private List<Book> owned = new ArrayList<>();
	private List<Book> wishlist = new ArrayList<>();

//##### METHODS #####	

	/**
	 * Sets a book as owned. In case the book is in the wishlist, it is removed and
	 * then added to the owned books.
	 * 
	 * @param book The book to be owned.
	 * @throws IllegalArgumentException if book passed is null.
	 */
	public void newOwn(Book book) {
		ParamCheck.isNotNull(book);

		if (wishlist.contains(book)) {
			wishlist.remove(book);
		}

		owned.add(book);
	}

	/**
	 * Moves a book into the  wishlist. In case the book is in owned, it is removed and
	 * then added to the wishlist.
	 * 
	 * @param book The book to be added to wishlist.
	 * @throws IllegalArgumentException if book passed is null.
	 */
	public void newWishlist(Book book) {
		ParamCheck.isNotNull(book);
		
		if(owned.contains(book)) {
			return;
		}
		
		wishlist.add(book);
	}

}
