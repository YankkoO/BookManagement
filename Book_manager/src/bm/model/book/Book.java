/**
 * @author Gómez Nido Gonzalo
 * @version 260210
 */
package bm.model.book;

import java.util.Date;
import java.util.Objects;

import kayanko.util.parameter.validation.ParamCheck;

/**
 * This class models books in the dataBase
 */
public class Book {
	
	private enum State{
		NOT_STARTED,
		STARTED,
		FINISHED
	}
	
	private String title; /*csv element at index 1*/
	private String author; /*csv element at index 2*/
	private double stars; /*csv element at index 6*/
	private String category; /*csv element at index 14*/
	private Date publishDate; /*csv element at index 15*/
	private State readState;
	private Review review;
	
//############################# CONSTRUCTORS #############################
	
	public Book(String title, String author, double stars,
				String category, Date publishDate,
				State readState) {
		
		this.setTitle(title);
		this.setAuthor(author);
		this.setStars(stars);
		this.setCategory(category);
		this.setPublishDate(publishDate);
		this.setReadState(readState);
	}
	
//############################# GETTERS & SETTERS #############################
	
	/**
	 * @return the title
	 */
	String getTitle() {
		return title;
	}
	
	/**
	 * @param title the title to set
	 */
	void setTitle(String title) {
		ParamCheck.notNullEmpty(title);
		this.title = title;
	}
	/**
	 * @return the author
	 */
	String getAuthor() {
		return author;
	}
	/**
	 * @param author the author to set
	 */
	void setAuthor(String author) {
		ParamCheck.notNullEmpty(author);
		this.author = author;
	}
	/**
	 * @return the stars
	 */
	double getStars() {
		return stars;
	}
	/**
	 * @param stars the stars to set
	 */
	void setStars(double stars) {
		ParamCheck.isTrue(stars >= 0.0 && stars <= 5.0, "reting format is invalid");
		this.stars = stars;
	}
	/**
	 * @return the category
	 */
	String getCategory() {
		return category;
	}
	/**
	 * @param category the category to set
	 */
	void setCategory(String category) {
		ParamCheck.notNullEmpty(category);
		this.category = category;
	}
	/**
	 * @return the publishDate
	 */
	Date getPublishDate() {
		return publishDate;
	}
	/**
	 * @param publishDate the publishDate to set
	 */
	void setPublishDate(Date publishDate) {
		this.publishDate = publishDate;
	}
	/**
	 * @return the readState
	 */
	State getReadState() {
		return readState;
	}
	/**
	 * @param readState the readState to set
	 */
	void setReadState(State readState) {
		this.readState = readState;
	}
	
	public Review getReview() {
		return review;
	}

	public void setReview(Review review) {
		ParamCheck.isNotNull(review);
		this.review = review;
	}

	
//############################# OVERRIDEN #############################
	
	@Override
	public int hashCode() {
		return Objects.hash(author, title);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Book other = (Book) obj;
		return Objects.equals(author, other.author) && Objects.equals(title, other.title);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Book [title=");
		builder.append(title);
		builder.append(", author=");
		builder.append(author);
		builder.append(", category=");
		builder.append(category);
		builder.append(", readState=");
		builder.append(readState);
		builder.append("]");
		return builder.toString();
	}
	
//############################# SPECIFIC METHODS #############################
	
	public boolean isCompleted() {
		return this.getReadState().equals(State.FINISHED);
	}
	
	
	
	
	
	
}
