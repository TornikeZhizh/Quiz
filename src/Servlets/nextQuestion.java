package Servlets;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import Models.Quiz;

/**
 * Servlet implementation class nextQuestion
 */
@WebServlet("/nextQuestion")
public class nextQuestion extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public nextQuestion() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Quiz q = (Quiz)request.getSession().getAttribute("A");
		if(q.isSingleQuestion()){
			q.setAnswer(q.getCurrentIndex(),(String)request.getParameter(""));
		} else {
			Enumeration<String> answers = request.getParameterNames();
			int i = 0;
			while(answers.hasMoreElements()){
				q.setAnswer(i++, answers.nextElement());
			}
			
		}
	}
	
	

}
