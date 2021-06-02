import java.io.PrintStream;
import java.util.Scanner;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.RollbackException;
import javax.persistence.TypedQuery;

import model.Book;
import model.Checkout;
import model.Department;
import model.Student;

public class Main {
	private static final Scanner in = new Scanner(System.in);
	private static final PrintStream out = System.out;

	public static void main(String[] args) {
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("LibraryDB");
		EntityManager em = emf.createEntityManager();

		displayMenu();
		loop: while (true) {
			switch (requestString("Selection (0 to quit, 6 for menu)? ")) {
			case "0": // Quit
				break loop;

			case "1": // Reset
				resetTables(em);
				break;

			case "2": 
				listAllBooksInDatabase(em);
				break;

			case "3": 
				selectBookToCheckout(em);
				break;

			case "4": 
				displayCheckoutList(em);
				break;

			case "5": // Add enrollment
				deleteCheckOutEntry(em);
				break;

			default:
				displayMenu();
				break;
			}
		}
		
		em.close();
		out.println("Done");
	}

	private static void displayMenu() {
		out.println("0: Quit");
		out.println("1: Reset tables");
		out.println("2: List all books available");
		out.println("3: Select a book to checkout");
		out.println("4: Display checked out books");
		out.println("5: Remove a checked out entry");
	}

	private static String requestString(String prompt) {
		out.print(prompt);
		out.flush();
		return in.nextLine();
	}

	private static int requestInt(String prompt) {
		out.print(prompt);
		out.flush();
		int result = in.nextInt();
		in.nextLine();
		return result;
	}
	
	@SuppressWarnings("unused")
	private static void resetTables(EntityManager em) {
		// Clear the tables
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		em.createQuery("delete from Checkout").executeUpdate();
		em.createQuery("delete from Book").executeUpdate();
		em.createQuery("delete from Student").executeUpdate();
		em.createQuery("delete from Department").executeUpdate();
		
		tryCommit(tx);
		
		em.clear(); // flush any locally-persisted objects
		
		// Now create the sample data objects and persist them
		tx = em.getTransaction();
		tx.begin();

		Department compsci = new Department("compsci", "thede", "julian");
		Department physics = new Department("physics", "kertzmann", "julian");
		Department english = new Department("english", "white", "asbury");
		
		em.persist(compsci);
		em.persist(physics);
		em.persist(english);

		Student taha = new Student("Taha Rabar", 1, compsci);
		Student vidit = new Student("Vidit Khandelwal", 2, english);
		Student kavya = new Student("Kavya", 3, physics);
		
		em.persist(taha);
		em.persist(vidit);
		em.persist(kavya);
		
		
		Book java = new Book("Java Fundamentals", "Lori White", 1, "Prevo", "24J", compsci);
		Book cplus = new Book("C++ Fundamentals", "Taha Babar", 2, "Prevo", "19K", compsci);
		Book python = new Book("Python Fundamentals", "Brian Howard", 3, "Prevo", "54F", compsci);
		Book quantam = new Book("Quantam Physics", "Omer Sajid", 4, "Prevo", "99K", physics);
		Book grammar = new Book("Grammar 101", "Isaac Newton", 5, "Roy O West", "1K", english);
		Book fiction = new Book("Harry Potter", "J.K. Rowling", 6, "Music", "3N", english);
			
		em.persist(java);
		em.persist(cplus);
		em.persist(python);
		em.persist(quantam);
		em.persist(grammar);
		em.persist(fiction);

		Checkout one = new Checkout(java, taha);
		
		em.persist(one);

		tryCommit(tx);
	}

	private static void tryCommit(EntityTransaction tx) {
		try {
			tx.commit();
		} catch (RollbackException ex) {
			ex.printStackTrace();
			tx.rollback();
		}
	}

	private static void listAllBooksInDatabase(EntityManager em) {
		out.printf("%-40s %-30s %-4s %-20s %-20s %-12s\n", "Book Name", "Author", "ID", "Pickup Library", "Pickup Shelf", "Department");
		out.println("----------------------------");
		
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		
		String query = "select b from Book b";
		TypedQuery<Book> q = em.createQuery(query, Book.class);
		

		for (Book book : q.getResultList()) {
			Department department = book.getDepartment();
			out.printf("%-40s %-30s %-4s %-20s %-20s %-12s\n", book.getBName(), book.getAuthor(), book.getBookId(), book.getPickupLibrary(), book.getPickupShelf(),
					(department != null) ? department.getDName() : "unknown");
		}
		
		tryCommit(tx);			
	}
	
	private static void selectBookToCheckout(EntityManager em) {
		
		int book = requestInt("Book ID? ");
		int student = requestInt("Student ID? ");
		
		
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		
		String query = "select b from Book b where b.bookId = ?1";
		TypedQuery<Book> q = em.createQuery(query, Book.class);
		q.setParameter(1, book);
		Book bookEntry = q.getSingleResult();
		
		query = "select s from Student s where s.sId = ?1";
		TypedQuery<Student> q2 = em.createQuery(query, Student.class);
		q2.setParameter(1, student);
		Student studentEntry = q2.getSingleResult();
		
		Checkout checkout = new Checkout(bookEntry, studentEntry);
		em.persist(checkout);
		
		tryCommit(tx);
		
	}
	
	private static void displayCheckoutList(EntityManager em) {
		out.printf("%-20s %-20s\n","Book Name", "Student Name");
		out.println("----------------------------");
		
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		
		String query = "select c from Checkout c";
		TypedQuery<Checkout> q = em.createQuery(query, Checkout.class);
		

		for (Checkout checkout : q.getResultList()) {
			Book book = checkout.getBook();
			Student student = checkout.getStudent();
			
			out.printf("%-20s %-20s\n",
					(book != null) ? book.getBookId() : "unknown", (student != null) ? student.getSId() : "unknown" );
		}
		
		tryCommit(tx);			
	}
	
	
	private static void deleteCheckOutEntry(EntityManager em) {
	int bId = requestInt("Book id number? ");
		
	EntityTransaction tx = em.getTransaction();
	tx.begin();

	String query = "delete from Checkout c where c.book.bookId = ?1";
	TypedQuery<Checkout> q = em.createQuery(query, Checkout.class);
	q.setParameter(1, bId);
	
	q.executeUpdate();

	tryCommit(tx);
	}
}


/*
 * 
 * Pseudo code for updating something!
 * 
 * 
 * 
 * private static void changeGrade(EntityManager em) {
		int eid = requestInt("Enrollment id number? ");
		String grade = requestString("New grade? ");

		EntityTransaction tx = em.getTransaction();
		tx.begin();

		Enroll enroll = em.find(Enroll.class, eid);
		enroll.setGrade(grade);
		
		tryCommit(tx);
		
		// TODO not checking for failure ...
	}
	
	
	*/
