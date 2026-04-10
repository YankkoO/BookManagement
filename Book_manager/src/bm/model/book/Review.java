package bm.model.book;

import kayanko.util.parameter.validation.ParamCheck;

/**
 * This class models a review made by the user. Not a given database review.
 */
public class Review {

	private String message;
	private int rating;
	private boolean favourite;
	
//############################# CONSTRUCTORS #############################
	
	public Review(String message, int rating, boolean fav) {
		this.setMessage(message);
		this.setRating(rating);
		this.setFavourite(fav);
		
	}
	
//############################# GETTERS & SETTERS #############################	
	
	/**
	 * @return the message
	 */
	String getMessage() {
		return message;
	}

	/**
	 * @return the rating
	 */
	int getRating() {
		return rating;
	}
	
	/**
	 * @return the favourite
	 */
	boolean isFavourite() {
		return favourite;
	}
	
	/**
	 * @param message the message to set
	 */
	void setMessage(String message) {
		ParamCheck.isNotNull(message);
		this.message = message;
	}
	
	/**
	 * @param rating the rating to set
	 */
	void setRating(int rating) {
		ParamCheck.isTrue(rating >= 0 && rating <= 10);
		this.rating = rating;
	}

	/**
	 * @param favourite the favourite to set
	 */
	void setFavourite(boolean favourite) {
		this.favourite = favourite;
	}

//############################# OVERRIDEN #############################	
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Review [message=");
		builder.append(message);
		builder.append(", rating=");
		builder.append(rating);
		builder.append(", favourite=");
		builder.append(favourite);
		builder.append("]");
		return builder.toString();
	}
	
	
	
	
}
