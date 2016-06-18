package Servlets;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import Models.Constants;
import Models.DBObject;
import Models.Quiz;

/**
 * Servlet implementation class Quizing
 */
@WebServlet("/Quizing")
public class Quizing extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Quizing() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		request.getRequestDispatcher(Constants.getAction(Constants.INDEX)).forward(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Quiz curQuiz = getCurrentQuiz(request, response);
		if (request.getParameter(Constants.QUIZINIG_DONE) != null) {
			doneQuiz(request, response, curQuiz);
		} else if (request.getParameter(Constants.QUIZINIG_NEXT) != null) {
			nextQuestion(request, response, curQuiz);
		} else if (request.getParameter(Constants.QUIZINIG_CHECK) != null) {
			checkAnswer(request, response, curQuiz);
		} else if (request.getParameter(Constants.QUIZINIG_CHECK_RESULT_NEXT_QUESTION) != null) {
			nextQuestionAfterCheck(response, request, curQuiz);
		}

		Quiz curQuiz = null;
		if (request.getParameter(Constants.QUIZINT_SINGLE_QUESTION) != null) {
			int singleQuestion = Integer.parseInt(request.getParameter(Constants.QUIZINT_SINGLE_QUESTION));
			int quizId = Integer.parseInt(request.getParameter(Constants.ATTR_QUIZ_ID_FOR_QUESTION));
			DBObject obj = (DBObject) getServletContext().getAttribute(DBObject.ATTR_DB);
			curQuiz = null;
			try {
				curQuiz = obj.getQuizById(quizId);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else {
			curQuiz = (Quiz) request.getSession().getAttribute(Constants.ATTR_SESSION_QUIZ);
			if (!curQuiz.isSingleQuestion()) {
				getAnswersAndCheck(curQuiz, request, response);
			} else {
				curQuiz.setAnswer(curQuiz.getCurrentIndex() - 1, request
						.getParameter(Constants.INDEX_DO_QUIZ_QUESTION_ANSWER + (curQuiz.getCurrentIndex() - 1)));
				if (!curQuiz.hasMoreQuestions()) {
					request.setAttribute(Constants.INDEX_DO_QUIZ_ATTR_RESULT_SCORE, curQuiz.getScore());
					request.setAttribute(Constants.INDEX_DO_QUIZ_ATTR_FINISHED, 1);
					curQuiz.restart();
				}
			}
		}
		redirectToQuizPage(curQuiz, request, response);
	}

	private void nextQuestionAfterCheck(HttpServletResponse response, HttpServletRequest request, Quiz curQuiz) {
		curQuiz.increaseQuestionCounter();
		redirectToQuizPage(curQuiz, request, response);
	}

	private void checkAnswer(HttpServletRequest request, HttpServletResponse response, Quiz curQuiz) {
		// TODO Auto-generated method stub
		
	}

	private void nextQuestion(HttpServletRequest request, HttpServletResponse response, Quiz curQuiz) {
		// TODO Auto-generated method stub
		
	}

	private void doneQuiz(HttpServletRequest request, HttpServletResponse response, Quiz curQuiz) throws ServletException, IOException {
		for (int i = 0; i < curQuiz.getQuestions().size(); i++) {
			String answer = request.getParameter(Constants.INDEX_DO_QUIZ_QUESTION_ANSWER + i);
			curQuiz.setUserAnswer(answer);
		}
		int score = curQuiz.getScore();
		request.setAttribute(Constants.INDEX_DO_QUIZ_ATTR_FINISHED, 1);
		request.setAttribute(Constants.INDEX_DO_QUIZ_ATTR_RESULT_SCORE, score);
		request.getRequestDispatcher(Constants.getAction("asd")).forward(request, response);
	}

	private Quiz getCurrentQuiz(HttpServletRequest request, HttpServletResponse response) {
		Quiz curQuiz = null;
		if (request.getParameter(Constants.QUIZINT_SINGLE_QUESTION) != null) {
			int singleQuestion = Integer.parseInt(request.getParameter(Constants.QUIZINT_SINGLE_QUESTION));
			int quizId = Integer.parseInt(request.getParameter(Constants.ATTR_QUIZ_ID_FOR_QUESTION));
			DBObject obj = (DBObject) getServletContext().getAttribute(DBObject.ATTR_DB);
			try {
				curQuiz = obj.getQuizById(quizId, singleQuestion);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			redirectToQuizPage(curQuiz, request, response);
		} else
			curQuiz = (Quiz) request.getSession().getAttribute(Constants.ATTR_SESSION_QUIZ);
		return curQuiz;
	}

	private void getAnswersAndCheck(Quiz curQuiz, HttpServletRequest request, HttpServletResponse response) {
		for (int i = 0; i < curQuiz.getQuestions().size(); i++) {
			String answer = request.getParameter(Constants.INDEX_DO_QUIZ_QUESTION_ANSWER + i);
			curQuiz.setAnswer(i, answer);
		}
		int score = curQuiz.getScore();
		request.setAttribute(Constants.INDEX_DO_QUIZ_ATTR_FINISHED, 1);
		request.setAttribute(Constants.INDEX_DO_QUIZ_ATTR_RESULT_SCORE, score);
		curQuiz.restart();
	}

	private void redirectToQuizPage(Quiz curQuiz, HttpServletRequest request, HttpServletResponse response) {
		try {
			request.getSession().setAttribute(Constants.ATTR_SESSION_QUIZ, curQuiz);
			request.getRequestDispatcher(Constants.getQuizURL(curQuiz.getID())).forward(request, response);
		} catch (ServletException | IOException e) {
			e.printStackTrace();
		}
	}

}
