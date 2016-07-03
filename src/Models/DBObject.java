package Models;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

import Questions.FillInBlankQuestion;
import Questions.MultipleChoiceQuestion;
import Questions.PictureQuestion;
import Questions.QuestionResponse;
import javafx.util.Pair;

public class DBObject {
	public static final String ATTR_DB = "ATTR_DB";
	public static final String MYSQL_USERNAME = DBInfo.MYSQL_USERNAME;
	public static final String MYSQL_PASSWORD = DBInfo.MYSQL_PASSWORD;
	public static final String MYSQL_DATABASE_SERVER = DBInfo.MYSQL_DATABASE_SERVER;
	public static final String MYSQL_DATABASE_NAME = DBInfo.MYSQL_DATABASE_NAME;

	public static final String TABLE_USERS = "users";
	public static final String TABLE_QUIZES = "quizes";
	public static final String TABLE_QUESTIONS = "questions";
	public static final String TABLE_QUIZ_LOGS = "quiz_logs";
	public static final String TABLE_CORRECT_ANSWERS = "correct_answers";
	public static final String TABLE_QUESTION_IMAGES = "question_images";
	public static final String TABLE_MULTIPLE_CHOICES = "multiple_choices";
	public static final String TABLE_FRIENDS = "friends";
	public static final String TABLE_MESSAGES = "messages";
	public static final String TABLE_CHALLENGES = "challenges";

	public static final int MESSAGE_TYPE_CHALLENGE = 0;
	public static final int MESSAGE_TYPE_TEXT_MESSAGE = 1;
	public static final int MESSAGE_SEEN = 1;
	public static final int MESSAGE_NOT_SEEN = 0;
	public static final int FRIEND_STATUS_PENDING = 0;
	public static final int FRIEND_STATUS_ACCEPTED = 1;

	public DBObject() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates and returns connection with the database
	 * 
	 * @return {@link Connection}
	 */
	private Connection getConnection() {
		try {
			String connect = "jdbc:mysql://" + MYSQL_DATABASE_SERVER + "/" + MYSQL_DATABASE_NAME;
			return DriverManager.getConnection(connect, MYSQL_USERNAME, MYSQL_PASSWORD);
		} catch (SQLException e) {
			System.out.println("a");
			e.printStackTrace();
			System.err.println("MySQL user password server or db name is incorrect!");
			return null;
		}
	}

	/**
	 * Closes given connection
	 * 
	 * @param conn
	 */
	private void closeConnection(Connection conn) {
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns result set generated by executing given query;
	 * 
	 * @param query
	 * @return {@link ResultSet}
	 */
	private ResultSet getResultSet(String query, Connection conn) {
		ResultSet result = null;
		try {
			Statement stmt = conn.createStatement();
			stmt.executeQuery("USE " + MYSQL_DATABASE_NAME);
			result = stmt.executeQuery(query);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Executes update queries, that is queries which cause changes in tables of
	 * the database;
	 * 
	 * @param query
	 * @throws SQLException
	 */
	private int executeUpdate(String query, Connection conn) {
		int id = 0;
		try {
			Statement stmt1;
			stmt1 = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
			stmt1.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = stmt1.getGeneratedKeys();
			if (rs.next()) {
				id = rs.getInt(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return id;
	}

	/**
	 * 
	 * Checks if user with given name or email already exists; If so, returns
	 * false, if such a user doesn't exist, adds the new user into users table.
	 * Uses executeUpdate; Method receives hashed password;
	 * 
	 * @param name
	 * @param email
	 * @param hashedPassword
	 * @return boolean
	 */
	public boolean addUser(String username, String email, String hashedPassword) {
		Connection conn = getConnection();
		if (userAlreadyExists(username, email, conn)) {
			System.out.println("User already exists");
			closeConnection(conn);
			return false;
		} else {
			System.out.println("User added successfully");
			String query = "INSERT INTO " + TABLE_USERS + " (user_name, email, password) VALUES " + "('" + username
					+ "', '" + email + "', '" + hashedPassword + "');";
			executeUpdate(query, conn);
			closeConnection(conn);
			return true;
		}
	}

	/**
	 * Checks if user with given name or email already exists;
	 * 
	 * @param name
	 * @param email
	 * @return boolean
	 */
	private boolean userAlreadyExists(String name, String email, Connection conn) {
		String query = "SELECT * FROM " + TABLE_USERS + " WHERE user_name = '" + name + "' or email = '" + email
				+ "' limit 1;";
		ResultSet r = getResultSet(query, conn);
		try {
			if (r.next())
				return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Get hashed password for user with given name;
	 * 
	 * @param userName
	 * @return {@link String}
	 * @throws SQLException
	 */
	public String getPasswordHash(String userName) {
		String result = null;
		Connection conn = getConnection();
		String query = "Select * from " + TABLE_USERS + " where user_name ='" + userName + "';";
		ResultSet rs = getResultSet(query, conn);
		try {
			if (rs.next())
				result = rs.getString("password");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		closeConnection(conn);
		return result;
	}

	/**
	 * Gets info of the user with given name
	 * 
	 * @param userName
	 * @return {@link HashMap}
	 */
	public HashMap<String, Object> getUserInfo(String userName) {
		HashMap<String, Object> result = new HashMap<String, Object>();
		Connection conn = getConnection();
		String query = "Select * from " + TABLE_USERS + " where user_name = '" + userName + "';";
		ResultSet rs = getResultSet(query, conn);
		try {
			if (rs.next()) {
				result.put("id", rs.getInt("id"));
				result.put("email", rs.getString("email"));
				result.put("reg_date", rs.getString("reg_date"));
				result.put("quizes_written", rs.getInt("quizes_written"));
				result.put("type", rs.getInt("type"));
			} else {
				System.out.println("User was not found in database!");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		closeConnection(conn);
		return result;
	}

	/**
	 * Gets quiz with given id from database and return it; Returns null if quiz
	 * with given id was not found;
	 * 
	 * @param id
	 * @param singleQuestion
	 * @return Quiz
	 * @throws SQLException
	 */
	public Quiz getQuizById(int id, int singleQuestion) throws SQLException {
		Connection conn = getConnection();
		String query = "SELECT quizes.*, users.user_name FROM " + TABLE_QUIZES + " quizes left join " + TABLE_USERS
				+ " users on quizes.author = users.id WHERE quizes.id = " + id + " limit 1;";
		ResultSet rs = getResultSet(query, conn);
		if (rs.next()) {
			String title = rs.getString("title");
			String description = rs.getString("description");
			String author = rs.getString("user_name");
			String createTime = rs.getString("create_time");
			int timesWritten = rs.getInt("times_written");
			boolean randomized = rs.getInt("randomize") == 1;
			boolean immediateCorrection = rs.getInt("immediate_correction") == 1;
			ArrayList<Question> questions = getQuestionsForQuiz(id, conn);
			boolean displaySingleQuestion = singleQuestion == 1;
			closeConnection(conn);
			return new Quiz(id, title, description, author, createTime, timesWritten, randomized, immediateCorrection,
					questions, displaySingleQuestion);
		} else {
			System.out.println("Quiz not found!");
			closeConnection(conn);
			return null;
		}
	}

	/**
	 * Get questions for quiz with given id; Assembles question infos from
	 * different tables depending on type of the question; Returns null if quiz
	 * contains no questions;
	 * 
	 * @param id
	 * @param conn
	 * @return
	 * @throws SQLException
	 */
	private ArrayList<Question> getQuestionsForQuiz(int id, Connection conn) throws SQLException {
		String query = "SELECT * FROM " + TABLE_QUESTIONS + " WHERE quiz_id = " + id + ";";
		ResultSet rs = getResultSet(query, conn);
		if (!rs.isBeforeFirst()) {
			return null;
		}
		ArrayList<Question> result = new ArrayList<Question>();
		while (rs.next()) {
			int qId = rs.getInt("id");
			result.add(getQuestionById(qId, conn));
		}
		return result;
	}

	/**
	 * Returns a question with given id and null if no such question was found;
	 * @param id
	 * @param conn
	 * @return
	 * @throws SQLException
	 */
	private Question getQuestionById(int id, Connection conn) throws SQLException {
		String query = "SELECT * FROM " + TABLE_QUESTIONS + " WHERE id = " + id + " limit 1;";
		ResultSet rs = getResultSet(query, conn);
		if (rs.next()) {
			String question = rs.getString("question");
			int type = rs.getInt("q_type");
			if (type == QuestionResponse.getType()) {
				return new QuestionResponse(question, getCorrectAnswers(id, conn));
			} else if (type == FillInBlankQuestion.getType()) {
				return new FillInBlankQuestion(question, getCorrectAnswers(id, conn));
			} else if (type == MultipleChoiceQuestion.getType()) {
				return new MultipleChoiceQuestion(question, getCorrectAnswers(id, conn), getPossibleAnswers(id, conn));
			} else if (type == PictureQuestion.getType()) {
				return new PictureQuestion(question, getCorrectAnswers(id, conn), getImageURL(id, conn));
			}
		}
		return null;
	}

	
	/**
	 * Returns the URL for a question with given id or empty string
	 * if no such URL was found;
	 * @param id
	 * @param conn
	 * @return
	 * @throws SQLException
	 */
	private String getImageURL(int id, Connection conn) throws SQLException {
		String imageURL = "SELECT * FROM " + TABLE_QUESTION_IMAGES + " WHERE question_id = " + id + ";";
		ResultSet rs = getResultSet(imageURL, conn);
		if (rs.next())
			return rs.getString("image_url");
		return "";
	}

	/**
	 * Get possible answers for question with given id
	 * TODO return null if no such question was found and check wherever we're calling this method;
	 * @param id
	 * @param conn
	 * @return
	 * @throws SQLException
	 */
	private ArrayList<String> getPossibleAnswers(int id, Connection conn) throws SQLException {
		String getPossibleAnswers = "SELECT * FROM " + TABLE_MULTIPLE_CHOICES + " WHERE question_id = " + id + ";";
		ResultSet possibleAnswers = getResultSet(getPossibleAnswers, conn);
		ArrayList<String> possibleAnswersList = new ArrayList<String>();
		while (possibleAnswers.next()) {
			String nextPossAnswer = possibleAnswers.getString("answer");
			possibleAnswersList.add(nextPossAnswer);
		}
		return possibleAnswersList;
	}

	/**
	 * Returns a list of correct answers for given question;
	 * TODO null check???
	 * @param id
	 * @param conn2
	 * @return
	 * @throws SQLException
	 */
	private ArrayList<String> getCorrectAnswers(int id, Connection conn) throws SQLException {
		String getCorrectAnswers = "SELECT * FROM " + TABLE_CORRECT_ANSWERS + " WHERE question_id = " + id + ";";
		ResultSet correctAnswers = getResultSet(getCorrectAnswers, conn);
		if (!correctAnswers.isBeforeFirst()) {
			throw new Error("No correct answers for this question in database");
		}
		ArrayList<String> result = new ArrayList<String>();
		while (correctAnswers.next()) {
			String nextAnswer = correctAnswers.getString("correct_answer");
			result.add(nextAnswer);
		}
		return result;
	}

	/**
	 * Gets specific info for different types of questions;
	 * 
	 * @param info
	 * @param qId
	 * @param qType
	 * @throws SQLException
	 */
	// private void getSpecificQuestionInfo(ArrayList<Object> info, int qId, int
	// qType) throws SQLException {
	// Connection conn = getConnection();
	// if (qType == QuestionType.MultipleChoice.ordinal()) {
	// String getPossibleAnswers = "SELECT * FROM " + TABLE_MULTIPLE_CHOICES + "
	// WHERE question_id = " + qId + ";";
	// ResultSet possibleAnswers = getResultSet(getPossibleAnswers, conn);
	// ArrayList<String> possibleAnswersList = new ArrayList<String>();
	// while (possibleAnswers.next()) {
	// String nextPossAnswer = possibleAnswers.getString("answer");
	// possibleAnswersList.add(nextPossAnswer);
	// }
	// info.add(2, possibleAnswersList);
	// } else if (qType == QuestionType.PictureResponse.ordinal()) {
	// String imageURL = "SELECT * FROM " + TABLE_QUESTION_IMAGES + " WHERE
	// question_id = " + qId + ";";
	// ResultSet url = getResultSet(imageURL, conn);
	// if (url.next()) {
	// info.add(2, url.getString("image_url"));
	// }
	// }
	// conn.close();
	// }

	/**
	 * Get several most popular quizzes in the database; If there are not as
	 * many quizzes in database as n, returns all the quizzes sorted according
	 * to popularity in descending order;
	 * 
	 * @param n
	 * @return {@link ArrayList}
	 * @throws SQLException
	 */
	public ArrayList<Pair<String, Integer>> getPopularQuizes(int n) throws SQLException {
		ArrayList<Pair<String, Integer>> popularQuizes = new ArrayList<Pair<String, Integer>>();
		Connection conn = getConnection();
		String query = "SELECT * FROM " + TABLE_QUIZES + " ORDER BY times_written DESC LIMIT " + n + ";";
		ResultSet rs = getResultSet(query, conn);
		if (!rs.isBeforeFirst())
			return null;
		while (rs.next()) {
			popularQuizes.add(new Pair<String, Integer>(rs.getString("title"), rs.getInt("id")));
		}
		closeConnection(conn);
		return popularQuizes;
	}

	/**
	 * Get n most recent quizzes in the database; If there are not as
	 * many quizzes in database as n, returns all the quizzes sorted 
	 * from most recent to older ones;
	 * @param userID
	 * @param n
	 * @return
	 * @throws SQLException
	 */
	public ArrayList<Pair<String, Integer>> getRecentQuizesForUser(int userID, int n) throws SQLException {
		ArrayList<Pair<String, Integer>> recentQuizesForUser = new ArrayList<Pair<String, Integer>>();
		Connection conn = getConnection();
		String query = "select quiz_id from " + TABLE_QUIZ_LOGS + " where user_id = " + userID
				+ " order by start_time desc limit " + n + ";";
		ResultSet rs = getResultSet(query, conn);
		if (!rs.isBeforeFirst())
			return null;
		while (rs.next()) {
			int id = rs.getInt("quiz_id");
			ResultSet r = getResultSet("Select title from quizes where id = " + id, conn);
			while (r.next()) {
				String title = r.getString("title");
				recentQuizesForUser.add(new Pair<String, Integer>(title, id));
			}
		}
		closeConnection(conn);
		return recentQuizesForUser;
	}

	/**
	 * Returns list of given number of recently created quizzes; If there are
	 * less than n quizzes in database, returns all the quizzes in the database;
	 * 
	 * @param n
	 * @return {@link ArrayList}
	 * @throws SQLException
	 */
	public ArrayList<Pair<String, Integer>> getRecentQuizes(int n) throws SQLException {
		ArrayList<Pair<String, Integer>> recentQuizes = new ArrayList<Pair<String, Integer>>();
		Connection conn = getConnection();
		String query = "SELECT * FROM " + TABLE_QUIZES + " ORDER BY create_time DESC LIMIT " + n + ";";
		ResultSet rs = getResultSet(query, conn);
		if (!rs.isBeforeFirst())
			return null;
		while (rs.next()) {
			recentQuizes.add(new Pair<String, Integer>(rs.getString("title"), rs.getInt("id")));
		}
		closeConnection(conn);
		return recentQuizes;
	}

	// This function inserts quiz in database
	public int addQuiz(String title, String description, boolean isRandomized, boolean isImmediateCorrection,
			int authorId) {
		int random = isRandomized ? 1 : 0;
		int immediateCorrection = isImmediateCorrection ? 1 : 0;
		Connection conn = getConnection();
		String query = "INSERT INTO " + TABLE_QUIZES
				+ " (title, description, author, randomize, immediate_correction) VALUES ('" + title + "', '"
				+ description + "', " + authorId + ", " + random + ", " + immediateCorrection + ");";
		int quizId = executeUpdate(query, conn);
		closeConnection(conn);
		return quizId;
	}

	// This function gets quizId and question and inserts this question in
	// database for the quiz
	public int addQuestionToQuiz(int quizId, Question question, int questionType) {
		Connection conn = getConnection();
		String quest = question.getQuestion();
		ArrayList<String> answers = question.getAnswers();
		int questionId = executeUpdate("INSERT INTO " + TABLE_QUESTIONS + " (quiz_id, question, q_type) VALUES ("
				+ quizId + ", '" + quest + "', " + questionType + ");", conn);
		for (int i = 0; i < answers.size(); i++)
			executeUpdate("INSERT INTO " + TABLE_CORRECT_ANSWERS + " (question_id, correct_answer) VALUES ("
					+ questionId + ", '" + answers.get(i) + "');", conn);
		if (questionType == MultipleChoiceQuestion.getType()) {
			ArrayList<String> possibleAnswers = question.getAdditionalData();
			if (possibleAnswers != null) {
				for (int i = 0; i < possibleAnswers.size(); i++)
					executeUpdate("INSERT INTO " + TABLE_MULTIPLE_CHOICES + " (question_id, answer) VALUES ("
							+ questionId + ", '" + possibleAnswers.get(i) + "');", conn);
			}
		} else if (questionType == PictureQuestion.getType()) {
			ArrayList<String> additionalData = question.getAdditionalData();
			if (additionalData != null && additionalData.size() == 1)
				executeUpdate("INSERT INTO " + TABLE_QUESTION_IMAGES + " (question_id, image_url) VALUES (" + questionId
						+ ", '" + additionalData.get(0) + "');", conn);
		}
		closeConnection(conn);
		return questionId;
	}
	
	/**
	 * Gets list of quizes by given author; TODO null check?????
	 * @param userId
	 * @return
	 */
	public ArrayList<Pair<String, Integer>> getQuizesListForUser(int userId) {
		ArrayList<Pair<String, Integer>> res = new ArrayList<Pair<String, Integer>>();
		Connection conn = getConnection();
		String getUserQuizes = "SELECT * FROM " + TABLE_QUIZES + " where author = " + userId + ";";
		ResultSet userQuizes = getResultSet(getUserQuizes, conn);
		try {
			while (userQuizes.next()) {
				String quizTitle = userQuizes.getString("title");
				int quizId = userQuizes.getInt("id");
				res.add(new Pair<String, Integer>(quizTitle, quizId));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		closeConnection(conn);
		return res;
	}

	/**
	 * Get user with given name;
	 * @param passed_username
	 * @return
	 */
	public User getUserByUserName(String passed_username) {
		Connection conn = getConnection();
		String query = "Select * from " + TABLE_USERS + " where user_name ='" + passed_username + "' ;";
		ResultSet rs = getResultSet(query, conn);
		User result = null;
		try {
			if (rs.next()) {
				int id = rs.getInt("id");
				String email = rs.getString("email");
				String regDate = rs.getString("reg_date");
				int quizesWritten = rs.getInt("quizes_written");
				int type = rs.getInt("type");
				result = new User(id, passed_username, email, regDate, quizesWritten, type, null);
				result.addFriends(getUserFriends(conn, result.getId()));
			} else {
				System.out.println("User was not found in database!");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		closeConnection(conn);
		return result;
	}

	/**
	 * Get user with given id;
	 * @param userId
	 * @return
	 * @throws SQLException
	 */
	public String getUserNameById(int userId) throws SQLException {
		Connection conn = getConnection();
		String query = "Select * from " + TABLE_USERS + " where id = " + userId;
		ResultSet rs = getResultSet(query, conn);
		if (!rs.isBeforeFirst())
			return null;
		String userName = "";
		if (rs.next())
			userName = rs.getString("user_name");
		closeConnection(conn);
		return userName;
	}

	/**
	 * Get user id for user with given name;
	 * @param userName
	 * @return
	 * @throws SQLException
	 */
	public int getUserIdByUserName(String userName) throws SQLException {
		Connection conn = getConnection();
		String query = "Select * from " + TABLE_USERS + " where user_name ='" + userName + "';";
		ResultSet rs = getResultSet(query, conn);
		if (!rs.isBeforeFirst())
			return -1;
		int id = -1;
		if (rs.next())
			id = rs.getInt("id");
		closeConnection(conn);
		return id;
	}

	/**
	 * Update friends table, mark that recipient has accepted sender's 
	 * friend request;
	 * @param recipient
	 * @param sender
	 */
	public void acceptFriendRequest(int recipient, int sender) {
		Connection conn = getConnection();
		String query1 = "UPDATE " + TABLE_FRIENDS + " SET status = " + FRIEND_STATUS_ACCEPTED + " WHERE (user1_id="
				+ sender + " AND user2_id=" + recipient + ");";
		executeUpdate(query1, conn);
		String query2 = "UPDATE " + TABLE_FRIENDS + " SET status = " + FRIEND_STATUS_ACCEPTED + " WHERE (user1_id="
				+ recipient + " AND user2_id=" + sender + ");";
		executeUpdate(query2, conn);
		closeConnection(conn);
	}

	private ArrayList<String> getUserFriends(Connection conn, int id) throws SQLException {
		ArrayList<String> friends = new ArrayList<String>();
		String query = "SELECT * FROM "+TABLE_FRIENDS + " WHERE (user1_id="+id+" OR user2_id="+id+") AND status="+FRIEND_STATUS_ACCEPTED+";";
		ResultSet rs = getResultSet(query, conn);
		if(!rs.isBeforeFirst()) {
			return null;
		}
		while(rs.next()) {
			int n1 = rs.getInt("user1_id");
			int n2 = rs.getInt("user2_id");
			int friendId = n1==id ? n2:n1;
			String friendName = this.getUserNameById(friendId);
			friends.add(friendName);
		}
		return friends;
	}

	/**
	 * Gets the list of pending friend requests for given user;
	 * 
	 * @param id
	 * @return
	 * @throws SQLException
	 */
	public ArrayList<String> getFriendRequestsForUser(int id) throws SQLException {
		Connection conn = getConnection();
		ArrayList<String> friendRequests = new ArrayList<String>();
		String query = "SELECT * FROM " + TABLE_FRIENDS + " WHERE user2_id = " + id + " and status = "
				+ FRIEND_STATUS_PENDING + ";";
		ResultSet rs = getResultSet(query, conn);
		if (!rs.isBeforeFirst()) {
			return null;
		}
		while (rs.next()) {
			int senderId = rs.getInt("user1_id");
			String userName = getUserNameById(senderId);
			friendRequests.add(userName);
		}
		closeConnection(conn);
		return friendRequests;
	}
	
	
	/**
	 * 
	 * @param id
	 * @return
	 * @throws SQLException
	 */
	public int getNumberOfFriendRequests(int id) throws SQLException {
		Connection conn = getConnection();
		int res = 0;
		String query = "SELECT COUNT(id) AS count FROM "+TABLE_FRIENDS+ " WHERE (user2_id="+id+" AND status="+FRIEND_STATUS_PENDING+");";
		ResultSet rs = getResultSet(query, conn);
		if(!rs.isBeforeFirst()) {
			return res;
		}
		rs.next();
		res = rs.getInt(1);
		closeConnection(conn);
		return res;
	}

	/**
	 * Returns true if one of the users has sent friend request to another;
	 * 
	 * @param id1
	 * @param id2
	 * @return
	 * @throws SQLException
	 */
	public boolean arePendingFriends(int id1, int id2) throws SQLException {
		Connection conn = getConnection();
		String query = "SELECT * FROM " + TABLE_FRIENDS + " WHERE (user1_id=" + id1 + " AND user2_id=" + id2
				+ ") OR (user1_id=" + id2 + " AND user2_id=" + id1 + ") AND status=" + FRIEND_STATUS_PENDING;
		ResultSet rs = getResultSet(query, conn);
		if (!rs.isBeforeFirst()) {
			return false;
		}
		closeConnection(conn);
		return true;
	}

	public void logQuiz(int user_id, int quiz_id, int score, long startTime, long thisQuizTime) {
		Connection conn = getConnection();
		String query = "INSERT INTO " + TABLE_QUIZ_LOGS + " (user_id, quiz_id, score, start_time, quizTime) VALUES ("
				+ user_id + ", " + quiz_id + ", " + score + ", " + startTime + ", " + thisQuizTime + ") ;";
		executeUpdate(query, conn);
		closeConnection(conn);
	}

	public void increaseQuizesWritten(int id) {
		Connection conn = getConnection();
		int timesWritten = getQuizTimesWritten(id, conn);
		if (timesWritten == -1)
			System.out.println("Error in MYSQL function: getQuizTimesWritten");
		String query = "UPDATE " + TABLE_QUIZES + " SET times_written = " + (timesWritten + 1) + " WHERE id = " + id
				+ ";";
		executeUpdate(query, conn);
		closeConnection(conn);
	}

	/**
	 * Get id of the sender and id of the message for given user;
	 * 
	 * @param userId
	 * @return
	 * @throws SQLException
	 */
	public ArrayList<Pair<Integer, Integer>> getMessages(int userId) throws SQLException {
		Connection conn = getConnection();
		String query = "select * from " + TABLE_MESSAGES + " where recipient = " + userId
				+ " order by receive_time desc;";
		ArrayList<Pair<Integer, Integer>> messages = new ArrayList<Pair<Integer, Integer>>();
		ResultSet rs = getResultSet(query, conn);
		if (!rs.isBeforeFirst())
			return null;
		try {
			while (rs.next()) {
				int senderId = rs.getInt("sender");
				int messageId = rs.getInt("id");
				messages.add(new Pair<Integer, Integer>(messageId, senderId));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		closeConnection(conn);
		return messages;
	}

	/**
	 * Return message with given id in the table of messages;
	 * 
	 * @param id
	 * @return
	 * @throws SQLException
	 */
	public Message getMessageById(int id) throws SQLException {
		Connection conn = getConnection();
		String query = "select * from " + TABLE_MESSAGES + " where id =" + id;
		ResultSet rs = getResultSet(query, conn);
		Message m = null;
		if (rs.next()) {
			String message = rs.getString("message_text");
			int senderId = rs.getInt("sender");
			int recipientId = rs.getInt("recipient");
			int messageId = rs.getInt("id");
			boolean seen = false;
			if (rs.getInt("seen") == 1)
				seen = true;
			String receiveTime = rs.getTimestamp("receive_time").toString();
			m = new Message(message, senderId, recipientId, seen, messageId, receiveTime);
		}
		closeConnection(conn);
		return m;

	}

	private int getQuizTimesWritten(int id, Connection conn) {
		String query = "SELECT * from " + TABLE_QUIZES + " WHERE id = " + id + " LIMIT 1;";
		ResultSet rs = getResultSet(query, conn);
		int result = -1;
		try {
			if (rs.next()) {
				result = rs.getInt("times_written");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Insert new text message to the table of messages;
	 * 
	 * @param sender
	 * @param recipient
	 * @param messageText
	 * @throws SQLException
	 */
	public void addSentMessage(String sender, String recipient, String messageText) throws SQLException {
		Connection conn = getConnection();
		int id1 = this.getUserIdByUserName(sender);
		int id2 = this.getUserIdByUserName(recipient);
		String query = "INSERT INTO messages (sender, recipient, type, message_text, seen) VALUES (" + id1 + ", " + id2
				+ ", " + MESSAGE_TYPE_TEXT_MESSAGE + ", '" + messageText + "', " + MESSAGE_NOT_SEEN + ");";
		this.executeUpdate(query, conn);
		closeConnection(conn);
	}

	/**
	 * Add row into table friends, status - pending friend request;
	 * 
	 * @param sender
	 * @param recipient
	 * @throws SQLException
	 */
	public void addFriendRequest(String sender, String recipient) throws SQLException {
		Connection conn = getConnection();
		int senderId = this.getUserIdByUserName(sender);
		int recipientId = this.getUserIdByUserName(recipient);
		String query = "INSERT INTO friends (user1_id, user2_id, status) VALUES (" + senderId + ", " + recipientId
				+ ", " + FRIEND_STATUS_PENDING + ");";
		this.executeUpdate(query, conn);
		closeConnection(conn);
	}

	public boolean usersAreFriends(int userId1, int userId2) throws SQLException {
		Connection conn = getConnection();
		String query = "SELECT * FROM " + TABLE_FRIENDS + " WHERE (user1_id=" + userId1 + " AND user2_id=" + userId2
				+ " AND status=" + FRIEND_STATUS_ACCEPTED + ") OR (user1_id=" + userId2 + " AND user2_id=" + userId1
				+ " AND status=" + FRIEND_STATUS_ACCEPTED + ");";
		ResultSet rs = getResultSet(query, conn);
		if (!rs.isBeforeFirst()) {
			return false;
		}
		closeConnection(conn);
		return true;
	}

	/**
	 * Remove friendship of given two users from the table of friends;
	 * 
	 * @param userName1
	 * @param userName2
	 * @throws SQLException
	 */
	public void removeFriend(String userName1, String userName2) throws SQLException {
		Connection conn = getConnection();
		int id1 = this.getUserIdByUserName(userName1);
		int id2 = this.getUserIdByUserName(userName2);
		String query = "DELETE FROM friends WHERE (user1_id=" + id1 + " and user2_id=" + id2 + ") or (user1_id=" + id2
				+ " and user2_id=" + id1 + ");";

		this.executeUpdate(query, conn);
		closeConnection(conn);
	}

	public String getSummaryForQuiz(int quizId, int userId) throws SQLException {
		Connection conn = getConnection();
		Quiz quiz = getQuizById(quizId, 1);
		String result = "Title: <B>" + quiz.getTitle() + "</B><BR>Description: " + quiz.getDescription();
		result += "<BR>Author: " + getQuizAuthorHTML(quizId);
		result += "<BR>Last 5 best quizers: " + getLastBestQuizers(quizId, 5, conn);
		result += "<BR>Last 10 quizers: " + getLastQuizers(quizId, 10, conn);
		result += "<BR>Your recent scores for this quiz: " + getRecentScore(quizId, userId, conn);
		closeConnection(conn);
		return result;
	}

	private String getRecentScore(int quizId, int userId, Connection conn) {
		String result = "<BR>Order by: <select onchange='newSort()' id='sortingUserQuizes'><option value='1' selected>Date</option><option value='2'>Score</option><option value='3'>Quiz Time</option></select>";
		result += "<div id='sorted1'>";
		result += getUsersRecentResultsForQuiz(quizId, userId, 1);
		result += "</div>";
		result += "<div id='sorted2' style='display: none'>";
		result += getUsersRecentResultsForQuiz(quizId, userId, 2);
		result += "</div>";
		result += "<div id='sorted3' style='display: none'>";
		result += getUsersRecentResultsForQuiz(quizId, userId, 3);
		result += "</div>";
		return result;
	}

	public String getUsersRecentResultsForQuiz(int quizId, int userId, int sort) {
		Connection conn = getConnection();
		String sorting = "start_time desc";
		if (sort == 2)
			sorting = "score desc, quizTime asc";
		if (sort == 3)
			sorting = "quizTime ASC";
		String result = "";
		String query = "SELECT * from " + TABLE_QUIZ_LOGS + " where user_id=" + userId + " and quiz_id=" + quizId
				+ " order by " + sorting + ";";
		ResultSet rs = getResultSet(query, conn);
		try {
			while (rs.next()) {
				Date d = new Date();
				int score = rs.getInt("score");
				long time = rs.getLong("quizTime") / 1000;
				if (sort == 3)
					System.out.println(time);
				long afterStart = (d.getTime() - rs.getLong("start_time")) / 1000;
				result += "<li>Score: " + score + " | Time: " + Constants.getTimeFromSecs(time) + " | Started: "
						+ Constants.getTimeFromSecs(afterStart) + " ago</li>";
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if (result.length() == 0)
			result = "You have not written this quiz yet!";
		closeConnection(conn);
		return result;
	}

	private String getLastBestQuizers(int quizId, int numberOfUsers, Connection conn) {
		String result = "";
		String query = "select u.user_name user_name, q.score score, q.start_time stime, q.quizTime msecs from "
				+ TABLE_QUIZ_LOGS + " q join " + TABLE_USERS + " u on q.user_id = u.id where quiz_id = " + quizId
				+ " order by score desc, quizTime asc limit " + numberOfUsers;
		ResultSet rs = getResultSet(query, conn);
		try {
			while (rs.next()) {
				Date d = new Date();
				int score = rs.getInt("score");
				long time = rs.getLong("msecs") / 1000;
				long afterStart = (d.getTime() - rs.getLong("stime")) / 1000;
				String username = rs.getString("user_name");
				result += "<li><a href='" + Constants.getUserProfileURL(username) + "' target='_blank' title='Time: "
						+ Constants.getTimeFromSecs(time) + ", Started: " + Constants.getTimeFromSecs(afterStart)
						+ " ago'><b>" + username + "</b></a>(" + score + " pts. "+(int)time+"sec)</li>";
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if (result.length() == 0)
			result = "Nobody has written this quiz yet!";
		return result;
	}

	private String getLastQuizers(int quizId, int numberOfUsers, Connection conn) {
		String result = "";
		String query = "select u.user_name user_name, q.score score, q.start_time stime, q.quizTime msecs from "
				+ TABLE_QUIZ_LOGS + " q join " + TABLE_USERS + " u on q.user_id = u.id where quiz_id = " + quizId
				+ " order by start_time desc limit " + numberOfUsers;
		ResultSet rs = getResultSet(query, conn);
		try {
			while (rs.next()) {
				Date d = new Date();
				int score = rs.getInt("score");
				long time = rs.getLong("msecs") / 1000;
				long afterStart = (d.getTime() - rs.getLong("stime")) / 1000;
				String username = rs.getString("user_name");
				result += "<li><a href='" + Constants.getUserProfileURL(username) + "' target='_blank' title='Time: "
						+ Constants.getTimeFromSecs(time) + " | Started: " + Constants.getTimeFromSecs(afterStart)
						+ " ago'><b>" + username + "</b></a> (" + score + " pts)</li>";
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if (result.length() == 0)
			result = "Nobody has written this quiz yet!";
		return result;
	}

	public ArrayList<String> getUserFriendsById(int id) throws SQLException {
		Connection conn = getConnection();
		String query = "Select * from " + TABLE_FRIENDS + " where (user1_id =" + id + " or user2_id =" + id
				+ ") and status =1";
		ResultSet rs = getResultSet(query, conn);
		ArrayList<String> friends = new ArrayList<String>();
		if (!rs.isBeforeFirst())
			return null;
		while (rs.next()) {
			int user1_id = rs.getInt("user1_id");
			int friend = user1_id == id ? rs.getInt("user2_id") : user1_id;
			friends.add(getUserNameById(friend));
		}
		closeConnection(conn);
		return friends;
	}

	public String getQuizAuthorHTML(int quizId) {
		Connection conn = getConnection();
		String result = "There is no quiz for id:" + quizId;
		String query = "SELECT u.user_name user_name FROM " + TABLE_QUIZES + " q join " + TABLE_USERS
				+ " u on q.author = u.id where q.id=" + quizId + " limit 1;";
		ResultSet rs = getResultSet(query, conn);
		try {
			if (rs.next()) {
				String username = rs.getString("user_name");
				result = "<a href='" + Constants.getUserProfileURL(username) + "' title='username' target='_blank'><b>"
						+ username + "</b></a>";
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		closeConnection(conn);
		return result;
	}
	
	public ArrayList<String> getUsersStartedWith(String username, int limit) {
		ArrayList<String> res = new ArrayList<>();
		Connection conn = getConnection();
		String query = "SELECT * from " + TABLE_USERS + " where user_name like '%" + username + "%' limit " + limit;
		ResultSet rs = getResultSet(query, conn);
		try {
			while (rs.next()) {
				String uName = rs.getString("user_name");
				res.add(uName);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		closeConnection(conn);
		return res;
	}
	
	public ArrayList<String> getFriendsStartedWith(String username, int limit,int id) throws SQLException {
		ArrayList<String> res = new ArrayList<>();
		ArrayList<String> friends = getUserFriendsById(id);
		int i = 0;
		for(String friend : friends){
			if(i >= limit) break;
			if(friend.contains(username)){
				res.add(friend);
				i++;
			}
		}
		return res;
	}
	
	public boolean addChallenge(String sender,String recipient,int quizID){
		Connection conn = getConnection();
		String query = "insert into " + TABLE_CHALLENGES + " (sender,recipient,quiz_id) values ('" + sender + "', '" + recipient +"', " + quizID +")";
		executeUpdate(query, conn);
		closeConnection(conn);
		return true;
	}
	
	public ArrayList<Pair<Integer, String>> getQuizesStartedWith (String quizname, int limit) {
		ArrayList<Pair<Integer, String>> res = new ArrayList<Pair<Integer, String>>();
		Connection conn = getConnection();
		String query = "SELECT * from " + TABLE_QUIZES + " where title like '%" + quizname + "%' limit " + limit;
		ResultSet rs = getResultSet(query, conn);
		try {
			while (rs.next()) {
				String qName = rs.getString("title");
				int id = rs.getInt("id");
				res.add(new Pair<Integer, String>(id, qName));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		closeConnection(conn);
		return res;
	}
	
	
	
	
	/**
	 * Returns a list of id-s of unseen messages;
	 * @param userId
	 * @return
	 * @throws SQLException
	 */
	public ArrayList<Integer> getUnseenMessages(int userId) throws SQLException {
		Connection conn = getConnection();
		String query = "SELECT * FROM "+TABLE_MESSAGES+" WHERE (recipient="+userId+" AND seen="+MESSAGE_NOT_SEEN+");";
		ResultSet rs = getResultSet(query, conn);
		if(!rs.isBeforeFirst()) {
			return null;
		}
		ArrayList<Integer> res = new ArrayList<Integer>();
		while(rs.next()) {
			res.add(rs.getInt("id"));
		}
		closeConnection(conn);
		return res;
	}
	
	
	
	public void markMessageAsSeen(int messageId) {
		Connection conn = getConnection();
		String query = "UPDATE "+TABLE_MESSAGES+" SET seen="+MESSAGE_SEEN+" WHERE id="+messageId+";";
		executeUpdate(query, conn);
		closeConnection(conn);
	}
	
	
	
	/**
	 * 
	 * @param userId
	 * @return
	 * @throws SQLException 
	 */
	public ArrayList<Challenge> getChallengesForUser(int userId) throws SQLException {
		Connection conn = getConnection();
		String query = "SELECT * FROM "+TABLE_CHALLENGES+" WHERE recipient_id = "+userId+" ORDER BY id DESC;";
		ResultSet rs = getResultSet(query, conn);
		if(!rs.isBeforeFirst()) {
			return null;
		}
		ArrayList<Challenge> challenges = new ArrayList<Challenge>();
		while(rs.next()) {
			int id = rs.getInt("id");
			int sender = rs.getInt("sender_id");
			int recipient = rs.getInt("recipient_id");
			int quiz = rs.getInt("quiz_id");
			int seen = rs.getInt("seen");
			String receiveTime = rs.getTimestamp("receive_time").toString();
			Challenge c = new Challenge(id, sender, recipient, quiz, seen, receiveTime);
			challenges.add(c);
		}
		closeConnection(conn);
		return challenges;
	}
	
	
	
	public int getNumberOfUnseenChallenges(String userName) throws SQLException {
		int res = 0;
		Connection conn = getConnection();
		String query = "SELECT COUNT(*) FROM "+TABLE_CHALLENGES+" WHERE (recipient='"+userName+"' AND seen="+MESSAGE_NOT_SEEN+");";
		ResultSet rs = getResultSet(query, conn);
		if(rs.isBeforeFirst()) {
			rs.next();
			res = rs.getInt(1);
		}
		closeConnection(conn);
		return res;
	}
	
	
	
	
	/**
	 * Gets best score that given user has achieved in given quiz; 
	 * If the user hasn't taken this quiz, returns -1;
	 * @param userId
	 * @param quizId
	 * @return
	 * @throws SQLException
	 */
	public int getBestScoreForUserInQuiz(int userId, int quizId) throws SQLException {
		int res = -1;
		Connection conn = getConnection();
		String query = "SELECT * FROM "+TABLE_QUIZ_LOGS+" WHERE user_id="+userId+" AND quiz_id="+quizId+" ORDER BY score DESC LIMIT 1;";
		ResultSet rs = getResultSet(query, conn);
		if(!rs.isBeforeFirst()) {
			return res;
		}
		while(rs.next()) {
			res = rs.getInt("score");
		}
		closeConnection(conn);
		return res;
	}
	
	
	public ArrayList<Pair<Integer, String>> getRecentQuizesCreatedBy(String userName, int n) throws SQLException {
		Connection conn = getConnection();
		String query = "SELECT * FROM "+TABLE_QUIZES+" WHERE author='"+this.getUserIdByUserName(userName)+
													"' ORDER BY create_time DESC LIMIT "+n+";";
		ResultSet rs = getResultSet(query, conn);
		if(!rs.isBeforeFirst()) {
			return null;
		}
		ArrayList<Pair<Integer, String>> res = new ArrayList<Pair<Integer, String>>();
		while(rs.next()) {
			res.add(new Pair<Integer, String>(rs.getInt("id"), rs.getString("title")));
		}
		closeConnection(conn);
		return res;
	}
}
