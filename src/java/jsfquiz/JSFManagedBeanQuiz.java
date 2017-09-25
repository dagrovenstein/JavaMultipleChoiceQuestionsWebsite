package jsfquiz;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author Daniel
 */
@Named(value = "quizBean")
@SessionScoped
public class JSFManagedBeanQuiz implements Serializable {

    ArrayList<String> chapterList = new ArrayList<>();
    //store answer key in an arrayList
    ArrayList<String> keyList = new ArrayList<>();
    ArrayList<String> keyAndExplanationList = new ArrayList<>();
    //store questions in an arrayList
    ArrayList<String> questionsList = new ArrayList<>();
    ArrayList<String> questionsListChecked = new ArrayList<>(); //for Individual view, check the answer user inputted.
    //store users answers in an arrayList
    ArrayList<String> usersAnswers = new ArrayList<>();
    HashSet<String> javaKeywords = new HashSet<>();

    ArrayList<Integer> questionNumbersForAllView = new ArrayList<>(); //ArrayList to hold all questions
    ArrayList<String> allCheckedQuestionsList = new ArrayList<>(); // holds when all questions checked

    ArrayList<String> radioButtonNames = new ArrayList<>();
    ArrayList<String> comboBoxesNames = new ArrayList<>();
    ArrayList<String> buttonNames = new ArrayList<>();

    String currentChapter;
    int chapterNumber; // 1 less than acutal chapter for index purposes
    int questionNumber;
    int currentQuestionNumber;
    String sectionStr = "";
    static String questionStr = "";
    String aBtn = "";
    String bBtn = "";
    String cBtn = "";
    String dBtn = "";
    String eBtn = "";
    String key = "";
    String explanation = "";
    String radioButtonValue = "";
    String checkBoxValues = "";
    String keyForCurrentQuestion = "";

    // for user bar graph statistics
    int totalCorrect = 0;
    int totalIncorrect = 0;
    int totalQuestions = 0;
    int totalUnanswered = 0;

    //Login variables for loginView
    private PreparedStatement pstmt;
    private Connection conn;
    private String username;
    private String password;
    private String firstname;
    private String mi;
    private String lastname;
    private String registerResponse;
    private String loginResponse;
    private boolean isLoggedIn = false;

    /**
     * Creates a new instance of JSFManagedBeanQuiz
     */
    public JSFManagedBeanQuiz() {
        initializeJdbc();
    }

    //================================LOGIN=====================================\\
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getMi() {
        return mi;
    }

    public void setMi(String mi) {
        this.mi = mi;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getLoginResponse() {
        return loginResponse;
    }

    public void setLoginResponse(String loginResponse) {
        this.loginResponse = loginResponse;
    }

    public String getRegisterResponse() {
        return registerResponse;
    }

    public void setRegisterResponse(String registerResponse) {
        this.registerResponse = registerResponse;
    }

    private void initializeJdbc() {
        try {
            // Load the JDBC driver
            Class.forName("com.mysql.jdbc.Driver");
            System.out.println("Driver loaded");
            // Establish a connection
            //conn = DriverManager.getConnection("jdbc:mysql://liang.armstrong.edu/grovenstein", "grovenstein", "tiger");
            // conn = DriverManager.getConnection("jdbc:mysql://localhost/selftest", "scott", "tiger");
            conn = DriverManager.getConnection("jdbc:mysql://us-cdbr-iron-east-04.cleardb.net:3306/heroku_e776c68d59d1bdf?reconnect=true", "b1c154c0428d00", "30a2e4ed");
            System.out.println("Database connected");
            // Create a Statement
            pstmt = conn.prepareStatement("insert into user "
                    + "(username, firstname, mi, lastname, password, whencreated) "
                    + "values (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)");

        } catch (ClassNotFoundException | SQLException ex) {
            registerResponse = "Error:" + ex;
            loginResponse = "Error:" + ex;
        }
    }

    public String returnToLogin() {
        return "http://sample-env.ctwzgka55a.us-west-2.elasticbeanstalk.com/faces/Login.xhtml";
    }

    public void createNewAccount() {
        if (username != null) {
            try {
                String query = "select * from user where username = '" + username + "';";
                ResultSet rs = pstmt.executeQuery(query);

                if (!rs.next()) {
                    insert(firstname, mi, lastname, username, password);
                    registerResponse = "Account created! Return to login page to login.";
                } else {
                    registerResponse = "Username already exists.<br> Please try another username.";
                }
            } catch (SQLException ex) {
                registerResponse = "Error: " + ex;
            }
        }
    }

    public void login() {
        if (username != null && password != null) {
            try {
                String query = "select * from user where username='" + username + "' and password= '" + password + "';";
                ResultSet rs = pstmt.executeQuery(query);
                if (!rs.next()) {
                    loginResponse = "Invalid username/password.";
                } else {
                    isLoggedIn = true;
                    //loginResponse = "Login success."; //login success
                    //temporary? Have to have the username in url?
                    FacesContext.getCurrentInstance().getApplication().getNavigationHandler().handleNavigation(FacesContext.getCurrentInstance(), null, "Chapters.xhtml?faces-redirect=true");
                }
            } catch (SQLException ex) {
                loginResponse = "Error: " + ex;
            }
        } else {
        }
    }

    private void insert(String firstname, String mi, String lastname, String username, String password) throws SQLException {
        pstmt.setString(1, username);
        pstmt.setString(2, firstname);
        pstmt.setString(3, mi);
        pstmt.setString(4, lastname);
        pstmt.setString(5, password);
        pstmt.executeUpdate();
    }
    
    //======================================QUIZ========================================\\

    // Get Request Parameter value (Ex: http://localhost:8080/Grovenstein/faces/Quiz.xhtml?chapter=1, urlParam contains chapter=1, currentChapter = chapter1(format of txt files), chapterNumber=1)
    public String getRequestParameter() {
        if (!isLoggedIn) {
            FacesContext.getCurrentInstance().getApplication().getNavigationHandler().handleNavigation(FacesContext.getCurrentInstance(), null, "NotLoggedIn.xhtml?faces-redirect=true");
        }

        //clear questionsList each new parameter
        questionsList.clear();
        //clear keyList each new parameter
        keyList.clear();
        //clear keyAndExplanationList each new parameter
        keyAndExplanationList.clear();
        try {
            String urlParam = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().toString();
            //urlParam = urlParam.replace("{", "").replace("}", "").replace("=", " ").replace("c", "C");
            urlParam = urlParam.replaceAll("[^\\d]", "");
            //check if a chapter was specified
            if (urlParam.isEmpty()) {
                FacesContext.getCurrentInstance().getApplication().getNavigationHandler().handleNavigation(FacesContext.getCurrentInstance(), null, "IncorrectChapter.xhtml?faces-redirect=true");
                return "Chapter not specified";
            }
            chapterNumber = Integer.parseInt(urlParam);
            currentChapter = "chapter" + chapterNumber;
            chapterNumber--; //for index purposes in chapterList
        } catch (NumberFormatException ex) {
        }
        if (chapterNumber > 43) {
            FacesContext.getCurrentInstance().getApplication().getNavigationHandler().handleNavigation(FacesContext.getCurrentInstance(), null, "IncorrectChapter.xhtml?faces-redirect=true");
            return "";
        } else {
            return chapterList.get(chapterNumber);
        }
    }

    public String getChapterComponentsUsingDB() {
        String allQuestions = "";
        String tempLine = "";
        populateHashSet(); // populate keywords
        try {
            ResultSet rs = pstmt.executeQuery("select * from intro10equiz where chapterNo=" + (chapterNumber + 1) + ";");
            while (rs.next()) {
                int chapterDB = rs.getInt("chapterNo");
                int questionNumDB = rs.getInt("questionNo");
                questionStr = rs.getString("question");
                aBtn = rs.getString("choiceA");
                aBtn = aBtn.toUpperCase().charAt(0) + aBtn.substring(1);
                bBtn = rs.getString("choiceB");
                bBtn = bBtn.toUpperCase().charAt(0) + bBtn.substring(1);
                cBtn = rs.getString("choiceC");
                cBtn = cBtn.toUpperCase().charAt(0) + cBtn.substring(1);
                dBtn = rs.getString("choiceD");
                dBtn = dBtn.toUpperCase().charAt(0) + dBtn.substring(1);
                eBtn = rs.getString("choiceE");
                eBtn = eBtn.toUpperCase().charAt(0) + eBtn.substring(1);
                key = rs.getString("answerKey");
                key = key.toUpperCase();
                String username = rs.getString("username");
                explanation = rs.getString("hint");

                for (String e : javaKeywords) {
                    if (questionStr.contains(e + " ") || (questionStr.contains(e + "[") || (questionStr.contains(e + " [")))) { // this will catch int[][] etc.
                        questionStr = questionStr.replaceAll(e + " ", "<font color=\"green\">" + e + " " + "</font>");
                    }
                }
                keyList.add(key); //add quetionNumber -1 to list so question 1 key is accessed by keyList(0);
                if (explanation.equals("null")) {
                    explanation = "";
                }
                keyAndExplanationList.add("The correct answer is: " + key + "<div style = 'color: purple'>" + explanation + "</div>");
                questionsList.add(createQuestion(sectionStr, questionStr, aBtn, bBtn, cBtn, dBtn, eBtn));
                explanation = "";
            }
        } catch (SQLException ex) {
            System.out.println("error: " + ex.toString());
        }
        for (String e : questionsList) {
            allQuestions += e;
        }
        return allQuestions;
    }

    //Make comboboxes, mutiple answers
    public String createQuestion(String sectionStr, String questionStr, String aBtn, String bBtn, String cBtn, String dBtn, String eBtn) {
        int questionNumber = questionsList.size() + 1; //user this to create names of radiobuttons/checkboxes/submit buttons to id Each question
        String radioButtons = "";
        String submitBtn = "";
        //add highlighting to sections if section exists
        if (!sectionStr.isEmpty()) {
            sectionStr = "<span style=\"background-color: #FFFF00\">" + sectionStr + "</span>";
        }
        //Make radio buttons, only one answer 
        if (key.length() == 1) {
            radioButtons = "<input type=\"radio\" name=\"q" + questionNumber + "\"  value =\"A\" />" + aBtn + "<br>\n"
                    + "<input type =\"radio\" name=\"q" + questionNumber + "\" value =\"B\" />" + bBtn + "<br>\n";
            if (!cBtn.equals("Null")) {
                radioButtons += "<input type=\"radio\" name=\"q" + questionNumber + "\" value =\"C\" />" + cBtn + "<br>\n";
            }
            if (!dBtn.equals("Null")) {
                radioButtons += "<input type=\"radio\" name=\"q" + questionNumber + "\" value =\"D\" />" + dBtn + "<br>\n";
            }
            if (!eBtn.equals("Null")) {
                radioButtons += "<input type=\"radio\" name=\"q" + questionNumber + "\" value =\"E\" />" + eBtn + "<br>\n";
            }
             submitBtn = "<div style=\"text-align: left; margin-right: 3em\"><input type=\"submit\" style=\"background: green; color: white; font-size: 85%; margin-top: 5px; margin-left: 35px; border: none\"<input type=\"submit\" name=\"btn" + questionNumber + "\" value=\"Check Answer for Question " + questionNumber + "\" action=\"#{quizSetup.getSelectedRadioButtonValue()}\"/></div>\n";
            //Make comboboxes, mutiple answers
        } else {
            radioButtons = "<input type =\"checkbox\" name =\"q" + questionNumber + "cb1\"  value =\"A\" />" + aBtn + "<br>\n"
                    + "<input type =\"checkbox\" name=\"q" + questionNumber + "cb2\" value =\"B\" />" + bBtn + "<br>\n";
            if (!cBtn.equals("Null")) {
                radioButtons += "<input type =\"checkbox\" name=\"q" + questionNumber + "cb3\" value =\"C\" />" + cBtn + "<br>\n";
            }
            if (!dBtn.equals("Null")) {
                radioButtons += "<input type =\"checkbox\" name=\"q" + questionNumber + "cb4\" value =\"D\" />" + dBtn + "<br>\n";
            }
            if (!eBtn.equals("Null")) {
                radioButtons += "<input type =\"checkbox\" name=\"q" + questionNumber + "cb5\" value =\"E\" />" + eBtn + "<br>\n";
            }
            submitBtn = "<div style=\"text-align: left; margin-right: 3em\"><input type=\"submit\" style=\"background: green; color: white; font-size: 85%; margin-top: 5px; margin-left: 35px; border: none\"<input type=\"submit\" name=\"btn" + questionNumber + "\" value=\"Check Answer for Question " + questionNumber + "\" action=\"#{quizSetup.getSelectedCheckBoxesValues()}\"/></div>\n";
        }

        return sectionStr + "<font size=\"3\" style=\"font-family:monospace, Verdana, Helvetica, sans-serif;\"><p style=\"margin-left: 35px\"><b style=\"color:purple; margin-left:-33px\">" + (chapterNumber + 1) + "." + questionStr.substring(0, 2).replaceAll("[^\\d]", "") + "</b>  " + questionStr.substring(3) + radioButtons + submitBtn + "</p>";
    }

    //temporary probably, but populates javaKeywords with keywords so they will be highlighted. (Problem, if keyword is in question not contatining code still highlights)
    public void populateHashSet() {
        javaKeywords.add("public");
        javaKeywords.add("static");
        javaKeywords.add("void");
        javaKeywords.add("private");
        javaKeywords.add("protected");
        javaKeywords.add("new");
        javaKeywords.add("double");
        javaKeywords.add("int");
        javaKeywords.add("float");
        javaKeywords.add("while");
        javaKeywords.add("for");
        javaKeywords.add("do");
        javaKeywords.add("public");
        javaKeywords.add("switch");
        javaKeywords.add("import");
        javaKeywords.add("this");
        javaKeywords.add("String");
        javaKeywords.add("if");
        javaKeywords.add("else");
        javaKeywords.add("true");
        javaKeywords.add("false");
        javaKeywords.add("boolean");
        javaKeywords.add("synchronized");
    }

    public String getRadioButtonValue() {
        return radioButtonValue;
    }

    public void getSelectedRadioButtonValue() {
        radioButtonValue = "";
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        for (int i = 1; i < questionsList.size() + 1; i++) {
            //all radio buttons for question 1 are named: q1, q1, q1, q1
            String rbValue = request.getParameter("q" + i);
            if (rbValue != null) {
                setRadioButtonValue(rbValue);
                FacesContext.getCurrentInstance().getApplication().getNavigationHandler().handleNavigation(FacesContext.getCurrentInstance(), null, "Question.xhtml?faces-redirect=true");
                //add user answer to question number and rbValue
                usersAnswers.add(i - 1, rbValue);
            }
        }
    }

    public void setRadioButtonValue(String radioButtonValue) {
        this.radioButtonValue = radioButtonValue;
    }

    public String getCheckBoxValues() {
        return checkBoxValues;
    }

    public void setCurrentChapter(String currentChapter) {
        this.currentChapter = currentChapter;
    }

    public String getCurrentChapter() {
        return currentChapter;
    }

    public void setChapterNumber(int chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public int getChapterNumber() {
        return chapterNumber;
    }

    public void setCheckBoxValues(String checkBoxValues) {
        this.checkBoxValues = checkBoxValues;
    }

    public void setKeyForCurrentQuestion(String keyForCurrentQuestion) {
        this.keyForCurrentQuestion = keyForCurrentQuestion;
    }

    public String getKeyForCurrentQuestion() {
        return keyForCurrentQuestion;
    }

    public void getSelectedCheckBoxesValues() {
        checkBoxValues = ""; //reinitialize checkboxvalues
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        for (int i = 1; i < questionsList.size() + 1; i++) {
            String cb1Value = request.getParameter("q" + (i) + "cb1");
            String cb2Value = request.getParameter("q" + (i) + "cb2");
            String cb3Value = request.getParameter("q" + (i) + "cb3");
            String cb4Value = request.getParameter("q" + (i) + "cb4");
            String cb5Value = request.getParameter("q" + (i) + "cb5");

            if (cb1Value == null) {
                cb1Value = "";
            } else {
                checkBoxValues += cb1Value;
            }
            if (cb2Value == null) {
                cb2Value = "";
            } else {
                checkBoxValues += cb2Value;
            }
            if (cb3Value == null) {
                cb3Value = "";
            } else {
                checkBoxValues += cb3Value;
            }
            if (cb4Value == null) {
                cb4Value = "";
            } else {
                checkBoxValues += cb4Value;
            }
            if (cb5Value == null) {
                cb5Value = "";
            } else {
                checkBoxValues += cb5Value;
            }
            setCheckBoxValues(checkBoxValues);
            usersAnswers.add(i - 1, checkBoxValues);
        }
    }

    //find out which answer was chosen by user for question and check them for the individual view
    public String checkUsersAnswersForIndividualView(int questionNumber) {
        int questionNum = questionNumber; //questionNumber was passed from method getQuestionForButtonPressed()
        int indexOfAnswer = 0;
        StringBuilder originalQuestion = new StringBuilder(questionsList.get(questionNum));

        //check user selected radio buttons
        if (keyList.get(questionNum).length() == 1) {
            indexOfAnswer = originalQuestion.toString().indexOf("value =\"" + usersAnswers.get(questionNum) + "\""); //a
            originalQuestion.insert(indexOfAnswer + 11, "checked");
        } else { //check user selected checkboxes
            String a, b, c, d, e = "";
            for (int i = 0; i < usersAnswers.get(questionNum).length(); i++) {
                if (i == 0) {
                    a = usersAnswers.get(questionNum).substring(i, 1); // (0,1) //a
                    indexOfAnswer = originalQuestion.toString().indexOf("value =\"" + a + "\""); //a
                    originalQuestion.insert(indexOfAnswer + 11, "checked");
                }
                if (i == 1) {
                    b = usersAnswers.get(questionNum).substring(i, 2); // (1,2) //b
                    indexOfAnswer = originalQuestion.toString().indexOf("value =\"" + b + "\""); //b
                    originalQuestion.insert(indexOfAnswer + 11, "checked");
                }
                if (i == 2) {
                    c = usersAnswers.get(questionNum).substring(i, 3); // (2,3) //c
                    indexOfAnswer = originalQuestion.toString().indexOf("value =\"" + c + "\""); //c
                    originalQuestion.insert(indexOfAnswer + 11, "checked");
                }
                if (i == 3) {
                    d = usersAnswers.get(questionNum).substring(i, 4); // (3,4) //d
                    indexOfAnswer = originalQuestion.toString().indexOf("value =\"" + d + "\""); //d
                    originalQuestion.insert(indexOfAnswer + 11, "checked");
                }
                if (i == 4) {
                    e = usersAnswers.get(questionNum).substring(i, 5); // (4,5) //e
                    indexOfAnswer = originalQuestion.toString().indexOf("value =\"" + e + "\""); //e
                    originalQuestion.insert(indexOfAnswer + 11, "checked");
                }
            }
        }
        //hide submit button from the question string for individual view
        int indexOfBtn = originalQuestion.indexOf("name=\"btn"); //index of button 
        originalQuestion.insert(indexOfBtn + 12, " hidden=\"\""); //hide button

        return originalQuestion.toString();
    }

    //Right or wrong method for individual questions
    public String rightOrWrongIndividual() {
        String response = "";
        if (usersAnswers.get(currentQuestionNumber - 1).isEmpty()) {
            response = "<center><font color=\"red\"> No answer was selected for Question " + currentQuestionNumber + "</font></center>";
        } //get value of button
        else if (usersAnswers.get(currentQuestionNumber - 1).equals(keyList.get(currentQuestionNumber - 1))) {
            response = "<font color=\"green\">Your answer is correct</font> " + "<img border=\"0\" src=\"http://morning-ridge-37817.herokuapp.com/check.png\" width=\"42\" height=\"30\">";
            storeUserAnswerInfoIntoDB(1, currentQuestionNumber);
        } else {
            response = "<font color=\"red\">Your answer " + usersAnswers.get(currentQuestionNumber - 1) + " is incorrect</font> " + "<img border=\"0\" src=\"http://morning-ridge-37817.herokuapp.com/x.png\" width=\"42\" height=\"30\">";
            response += "<div style=\"text-align: left; margin-right: 1em\"><input type=\"submit\" style=\"background: green; color: white; font-size: 85%; margin-top: 5px; margin-left: 5px; border: none\"<input type=\"submit\" name=\"keyBtn\" value=\"Answer to Question " + currentQuestionNumber + "\"/></div>";
            storeUserAnswerInfoIntoDB(0, currentQuestionNumber);
        }
        return response;
    }

    //Right or wrong method used for all questions
    public String rightOrWrongAll(int questionNumber) {
        String response = "";
        if (usersAnswers.get(questionNumber - 1).isEmpty()) {
            response = "<center><font color=\"red\"> No answer was selected for Question " + questionNumber + "</font></center>";
        } //get value of button
        else if (usersAnswers.get(questionNumber - 1).equals(keyList.get(questionNumber - 1))) {
            response = "<font color=\"green\">Your answer is correct</font> " + "<img border=\"0\" src=\"http://morning-ridge-37817.herokuapp.com/check.png\" width=\"42\" height=\"30\"><br>";
            totalCorrect++;
            storeUserAnswerInfoIntoDB(1, questionNumber);
        } else {
            totalIncorrect++;
            response = "<font color=\"red\">Your answer " + usersAnswers.get(questionNumber - 1) + " is incorrect</font> " + "<img border=\"0\" src=\"http://morning-ridge-37817.herokuapp.com/x.png\" width=\"42\" height=\"30\">";
            response += "<br><font color=\"green\">" + keyAndExplanationList.get(questionNumber - 1) + "</font><br>";
            storeUserAnswerInfoIntoDB(0, questionNumber);
        }
        return response;
    }

    //figure out which question to show on individual qustion view according to button pressed
    public String getQuestionForButtonPressed() {
        String allAnswers = "";
        String checkAnswerBtn = "";
        String checkAllAnswerBtn = "";
        String allBtn = "";
        try {
            HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
            //check for which button isn't null (which means it was pressed)
            for (int i = 1; i <= questionsList.size(); i++) {
                checkAnswerBtn = request.getParameter("btn" + i);
                allBtn = request.getParameter("allBtn");
                if (checkAnswerBtn != null) {
                    break;
                }
                //allBtn pressed with no answers then just show graph
                if (allBtn != null) {
                    totalIncorrect = 0;
                    totalCorrect = 0;
                    totalQuestions = 0;
                    totalUnanswered = 0;
                    totalQuestions = questionsList.size();
                    totalUnanswered = questionsList.size() - (totalCorrect - totalIncorrect);
                    return barGraph();
                }
            }
            int questionNumberSubmitted = Integer.parseInt(checkAnswerBtn.replaceAll("[^\\d]", ""));
            currentQuestionNumber = questionNumberSubmitted; //acutal question number need to subtract 1 from this for arraylist index purposes. q1 = 0
            //if user didnt submit an answer don't return anything rightOrWrongAnswer() will handle this.
            if (usersAnswers.get(currentQuestionNumber - 1).isEmpty()) {
                return "<center><font color=\"red\"> No answer was selected for Question " + currentQuestionNumber + "</font></center>";
            }
            //otherwise return the string containing radio/checkboxes with user selected values checked.
            return checkUsersAnswersForIndividualView(currentQuestionNumber - 1) + rightOrWrongIndividual(); // questionNumberSubmitted - 1 because arraylist index starts from 0
        } catch (NullPointerException ex) {
            //means explanation button was pressed just return same return statement....
            return checkUsersAnswersForIndividualView(currentQuestionNumber - 1) + rightOrWrongIndividual();
        }
    }

    //display either single question or all questions according to check all questions button or single button 
    public String getQuestionForAllOrIndividual() {
        String allCheckedQuestions = "";
        if (allCheckedQuestionsList.isEmpty()) {
            allCheckedQuestionsList.clear();
            questionNumbersForAllView.clear();
            return getQuestionForButtonPressed();
        } else {
            //reinitalize totals
            totalIncorrect = 0;
            totalCorrect = 0;
            totalQuestions = 0;
            totalUnanswered = 0;
            setKeyForCurrentQuestion(""); // empty since key is handled without a button for multiview. with this there will be a repeat at the botto mof the last answered question
            for (int i = 0; i < questionNumbersForAllView.size(); i++) {
                allCheckedQuestions += allCheckedQuestionsList.get(i) + rightOrWrongAll(questionNumbersForAllView.get(i));
            }
        }
        totalQuestions = questionsList.size();
        totalUnanswered = questionsList.size() - (totalIncorrect + totalCorrect);
        return barGraph() + allCheckedQuestions;
    }

    public String barGraph() {
        return "<br><style>.barcontainer {\n"
                + "  position: relative;\n"
                + "  border: 3px solid black;\n"
                + "  border-radius: 5px 5px 0 0;\n"
                + "  width: 60%;\n"
                + "  margin: 0 auto;\n"
                + "  min-height: 12vw;\n"
                + "  max-height: 12vw;\n"
                + "  min-width: 60%;\n"
                + "  max-width: 60%;\n"
                + "  z-index: 1;\n"
                + "}\n"
                + "\n"
                + ".barcontainerheader {\n"
                + "  display: inline;\n"
                + "  position: absolute;\n"
                + "  width: 100%;\n"
                + "  padding-top: 3px;\n"
                + "  padding-bottom: 3px;\n"
                + "  z-index: 0;\n"
                + "}\n"
                + "\n"
                + ".bar {\n"
                + "  position: absolute;\n"
                + "  display: inline-block;\n"
                + "  bottom: 0;\n"
                + "  border: 1px solid black;\n"
                + "  border-radius: 2px 2px 0 0;\n"
                + "  width: 14%;\n"
                + "  text-align: center;\n"
                + "  color: white;\n"
                + "\n"
                + "}\n"
                + "\n"
                + ".barlabel {\n"
                + "  position: absolute;\n"
                + "  border-top: 2px solid black;\n"
                + "  background: #888;\n"
                + "  bottom: 0;\n"
                + "  width: 100%;\n"
                + "  text-shadow: 1px 1px 0px black;\n"
                + "  color: white;\n"
                + "}\n"
                + "\n"
                + ".bar:nth-child(2) {\n"
                + "   background: #0000FF;\n"
                + "  left: 5%;\n"
                + "}\n"
                + "\n"
                + ".bar:nth-child(3) {\n"
                + "    background: #00FF00;\n"
                + "  left: 20%;\n"
                + "}\n"
                + "\n"
                + ".bar:nth-child(4) {\n"
                + "    background: #FF0000;\n"
                + "  left: 35%;\n"
                + "}\n"
                + "\n"
                + ".bar:nth-child(5) {\n"
                + "    background: #FFFFFF;\n"
                + "  left: 50%;\n"
                + "}\n"
                + ".bar:nth-child(6) {\n"
                + "    border: white;\n"
                + "}\n"
                + "\n"
                + ".bar:nth-child(7) {\n"
                + "    border: white;\n"
                + "    background: white;\n"
                + "    width:30%;\n"
                + "  left: 70%;\n"
                + "}\n"
                + "</style>\n"
                + "<div class='barcontainer'>\n"
                + "  <div class='barcontainerheader'>\n"
                + "  </div>\n"
                + "  <div class='bar' style='height:" + ((totalQuestions*90)/totalQuestions) + "%'>\n"
                + "    <div class='barlabel'>\n"
                + "      Total\n"
                + "    </div>\n"
                + "  </div>\n"
                + "  <div class='bar' style='height:" + ((totalCorrect*90)/totalQuestions) + "%'>\n"
                + "    <div class='barlabel'>\n"
                + "      Correct\n"
                + "    </div>\n"
                + "  </div>\n"
                + "  <div class='bar' style='height:" + ((totalIncorrect*90)/totalQuestions) + "%'>\n"
                + "    <div class='barlabel'>\n"
                + "     Incorrect\n"
                + "    </div>\n"
                + "  </div>\n"
                + "  <div class='bar' style='height:" + ((totalUnanswered*90)/totalQuestions) + "%'>\n"
                + "    <div class='barlabel'>\n"
                + "      Unanswered\n"
                + "    </div>\n"
                + "  </div>\n"
                + "     <div class='bar' style='height:0%'>\n"
                + "  </div>\n"
                + "  <div class='bar' style='height:95%'>\n"
                + "      <div align=\"center\"><p align=\"left\"><b><font color=\"blue\" style=\"font-size: 100%\">Total Questions: " + totalQuestions + "</font><br>\n"
                + "                  <font color=\"limegreen\" style=\"font-size: 100%\">Total Correct: " + totalCorrect + "</font><br>\n"
                + "                  <font color=\"red\" style=\"font-size: 100%\">Total Incorrect: " + totalIncorrect + "</font><br>\n"
                + "                          <font color=\"black\" style=\"font-size: 100%\">Total Unanswered: " + totalUnanswered + "</font></b></p></div>\n"
                + "    </div>\n"
                + "  </div>"
                + "  </div>";
    }
    
    //Get scoped variable for a single question
    public void getRequestParameterIndividualView() {
        String urlParam = "";
        try {
            urlParam = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().toString();
            if (urlParam.contains("All")) {
                getRequestParameterAllView(urlParam); // this is getting the scoped vairables
            } else { // comment out iif it doesnt work
                getRequestParameterAllView(urlParam);//IMPLEMENT BETTER LATER. Fixes the problem with not being able to change previous answer if you select an answer for the current question issue involves
                //simply not grabbing all radio button values, clean this code up later maybe combine both getRequestParameter methods.
                allCheckedQuestionsList.clear();
                questionNumbersForAllView.clear();
                int currentButtonSubmitted = Integer.parseInt(urlParam.substring(urlParam.length() - 3, urlParam.length() - 1).replaceAll(" ", "")); // example: Check answer for question 1 => 1
                //answer key button
                if (urlParam.contains("keyBtn")) {
                    setKeyForCurrentQuestion(keyAndExplanationList.get(currentButtonSubmitted - 1));
                    //if urlParam does not contain {q + question number then question wasnt answered this works for checkboxes and radio buttons
                } else {
                    setKeyForCurrentQuestion(""); // set key to empty so it doesn't show for next question
                }
                if (!urlParam.contains("q" + currentButtonSubmitted)) {
                    //  radioButtonValue = "Question was no answered" + "URL Param: " + urlParam;
                } //radio buttons
                else if (keyList.get(currentButtonSubmitted - 1).length() == 1) { // -1 for arraylist index purposes
                    int indexOfRadioButtonAnswer = urlParam.indexOf("q" + currentButtonSubmitted);
                    //index has to be two due to index shift when question number is two digits
                    String rbanswer = urlParam.substring(indexOfRadioButtonAnswer + 3, indexOfRadioButtonAnswer + 5).replaceAll("[^a-zA-Z]", ""); //replace all non letters
                    // if user has already answered set that user answer to empty adn replace with new anwswer
                    if (!usersAnswers.get(currentButtonSubmitted - 1).isEmpty()) {
                        usersAnswers.remove(currentButtonSubmitted - 1);
                    }
                    usersAnswers.add(currentButtonSubmitted - 1, rbanswer);
                    //  radioButtonFormat in post is: {q1=a, btn1=Check Answer for Question 1}
                    // checkBoxFormat in post is: {q3cb1=a, q3cb2=b, q3cb3=c, q3cb4=d, btn3=Check Answer for Question 3}
                    // usersAnswers.add(0, urlParam);
                    //   setRadioButtonValue(urlParam + " CURRENT: " + currentButtonSubmitted + "RADIOBUTTONVALUE:" + rbanswer + " Current chapter: " + currentChapter + "KEY: " + keyForCurrentQuestion);
                    //checkboxes
                } else if (keyList.get(currentButtonSubmitted - 1).length() > 1) {
                    String cbanswer = "";
                    int indexOfCheckBoxA = urlParam.indexOf("q" + currentButtonSubmitted + "cb1");
                    int indexOfCheckBoxB = urlParam.indexOf("q" + currentButtonSubmitted + "cb2");
                    int indexOfCheckBoxC = urlParam.indexOf("q" + currentButtonSubmitted + "cb3");
                    int indexOfCheckBoxD = urlParam.indexOf("q" + currentButtonSubmitted + "cb4");
                    int indexOfCheckBoxE = urlParam.indexOf("q" + currentButtonSubmitted + "cb5");
                    if (!usersAnswers.get(currentButtonSubmitted - 1).isEmpty()) {
                        usersAnswers.remove(currentButtonSubmitted - 1);
                    }
                    if (indexOfCheckBoxA != -1) {
                        cbanswer += urlParam.substring(indexOfCheckBoxA + 6, indexOfCheckBoxA + 8).replaceAll("[^a-zA-Z]", ""); //index is distance of two because of index shift when question
                        //number is two digits.....aka q1cb1 and q14cb1 has different indexes to grab answer.
                    }
                    if (indexOfCheckBoxB != -1) {
                        cbanswer += urlParam.substring(indexOfCheckBoxB + 6, indexOfCheckBoxB + 8).replaceAll("[^a-zA-Z]", "");
                    }
                    if (indexOfCheckBoxC != -1) {
                        cbanswer += urlParam.substring(indexOfCheckBoxC + 6, indexOfCheckBoxC + 8).replaceAll("[^a-zA-Z]", "");
                    }
                    if (indexOfCheckBoxD != -1) {
                        cbanswer += urlParam.substring(indexOfCheckBoxD + 6, indexOfCheckBoxD + 8).replaceAll("[^a-zA-Z]", "");
                    }
                    if (indexOfCheckBoxE != -1) {
                        cbanswer += urlParam.substring(indexOfCheckBoxE + 6, indexOfCheckBoxE + 8).replaceAll("[^a-zA-Z]", "");
                    }
                    usersAnswers.add(currentButtonSubmitted - 1, cbanswer);
                }

                //setRadioButtonValue(urlParam + " CURRENT: " + currentButtonSubmitted);
            }
        } catch (NumberFormatException ex) {
            //empty catch due to null string when chapter is not specified
        }
    }

    //Method for prepping of display of all questions on check all button, parses the url param requests and gets which numbers were answered and store the questionNumbers into an arraylist
    public void getRequestParameterAllView(String urlParam) {
        String allString = "";
        questionNumbersForAllView.clear();
        allCheckedQuestionsList.clear();
        try {
            //THe format: {q1=B, q2=C, q3cb2=B, q3cb3=C, q4=C, q5=B, allBtn=Check Answer for All Questions}
            //reinialize key for every question so its not displayed until button pressed
            //this can be referenced as the question number
            for (int i = 1; i <= questionsList.size(); i++) { // scan entire urlParam for possible answered questions for all answers view
                String cbAnswer = "";
                //for radio buttons 
                if (urlParam.contains("q" + i + "=")) { //radio button selected
                    questionNumbersForAllView.add(i);
                    int indexOfRadio = urlParam.indexOf("q" + i + "=");
                    String rbAnswer = urlParam.substring(indexOfRadio + 3, indexOfRadio + 5).replaceAll("[^a-zA-Z]", "");  // radio button answer for this specific question
                    if (!usersAnswers.get(i - 1).isEmpty()) {
                        usersAnswers.remove(i - 1);
                    }
                    usersAnswers.add(i - 1, rbAnswer);
                } else {
                    //add checkbox to allview if any checkbox is present for i
                    if (urlParam.contains("q" + i + "cb1") || urlParam.contains("q" + i + "cb2") || urlParam.contains("q" + i + "cb3")
                            || urlParam.contains("q" + i + "cb4") || urlParam.contains("q" + i + "cb5")) {
                        questionNumbersForAllView.add(i); //if any checkbox exits containing q+i then add that quetion to all view.
                        if (!usersAnswers.get(i - 1).isEmpty()) {
                            usersAnswers.remove(i - 1);
                        }

                        //its juts getting same answer everytime and storing in different places according to the question number?
                        //for checkboxes
                        if (urlParam.contains("q" + i + "cb1")) { //checkboxes selected
                            int indexOfCheckBoxA = urlParam.indexOf("q" + i + "cb1");
                            cbAnswer += urlParam.substring(indexOfCheckBoxA + 6, indexOfCheckBoxA + 8).replaceAll("[^a-zA-Z]", "");
                        }
                        if (urlParam.contains("q" + i + "cb2")) {
                            int indexOfCheckBoxB = urlParam.indexOf("q" + i + "cb2");
                            cbAnswer += urlParam.substring(indexOfCheckBoxB + 6, indexOfCheckBoxB + 8).replaceAll("[^a-zA-Z]", "");
                        }
                        if (urlParam.contains("q" + i + "cb3")) {
                            int indexOfCheckBoxC = urlParam.indexOf("q" + i + "cb3");
                            cbAnswer += urlParam.substring(indexOfCheckBoxC + 6, indexOfCheckBoxC + 8).replaceAll("[^a-zA-Z]", "");
                        }
                        if (urlParam.contains("q" + i + "cb4")) {
                            int indexOfCheckBoxD = urlParam.indexOf("q" + i + "cb4");
                            cbAnswer += urlParam.substring(indexOfCheckBoxD + 6, indexOfCheckBoxD + 8).replaceAll("[^a-zA-Z]", "");
                        }
                        if (urlParam.contains("q" + i + "cb5")) {
                            int indexOfCheckBoxE = urlParam.indexOf("q" + i + "cb5");
                            cbAnswer += urlParam.substring(indexOfCheckBoxE + 6, indexOfCheckBoxE + 8).replaceAll("[^a-zA-Z]", "");
                        }
                    }
                }

                if (!cbAnswer.isEmpty()) {
                    usersAnswers.add(i - 1, cbAnswer);
                }
            }

            //put checkmarks for users answers chosen for the questions they answered.
            for (int e : questionNumbersForAllView) {
                allCheckedQuestionsList.add(checkUsersAnswersForIndividualView(e - 1)); //has indiviudal here but this allview fives the reason why answer is :ABABCD?
            }

        } catch (NumberFormatException ex) {
            //empty catch due to null string when chapter is not specified

        }
    }

    //return string to build a table containing all chapter names as pressable text to go to quiz
    public String populateChapterListForLoginView() {
        String totalChapters = "";
        if (chapterList.isEmpty()) {
            try {
                URL url = new URL("https://morning-ridge-37817.herokuapp.com/chapterTitles.txt");
                Scanner scanner = new Scanner(url.openStream());
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    chapterList.add(line);
                }
            } catch (MalformedURLException ex) {
                System.out.println("URL not working...");
            } catch (IOException ex) {
            }
        }
        for (String e : chapterList) {
            String chapterNumberStr = e.replaceAll("[^\\d]", "");
            int chapterNumber = Integer.parseInt(chapterNumberStr);
            if (chapterNumber == 4224) //chapter 42 title contains numbers....
            {
                chapterNumber = 42;
            }
            totalChapters += "<a href=\"http://sample-env.ctwzgka55a.us-west-2.elasticbeanstalk.com/faces/Quiz.xhtml?chapter=" + chapterNumber + "&username=" + username + "\"  style=\"text-decoration:none;\">" + e + " </a><br>";
        }
        return totalChapters;
    }

    public void storeUserAnswerInfoIntoDB(int isCorrect, int questionNumber) {
        int answerA = 0;
        int answerB = 0;
        int answerC = 0;
        int answerD = 0;
        int answerE = 0;
        String hostName = "";
        //  int isCorrect = 0;

        String userAnswer = usersAnswers.get(questionNumber - 1);

        if (userAnswer.contains("A")) {
            answerA = 1;
        }
        if (userAnswer.contains("B")) {
            answerB = 1;
        }
        if (userAnswer.contains("C")) {
            answerC = 1;
        }
        if (userAnswer.contains("D")) {
            answerD = 1;
        }
        if (userAnswer.contains("E")) {
            answerE = 1;
        }

        if (usersAnswers.get(questionNumber - 1).equals(keyList.get(questionNumber - 1))) {
            isCorrect = 1;
        }
        try {

            String query = "select * from intro10e where chapterNo=" + (chapterNumber + 1) + " and questionNo=" + questionNumber + " and username= '" + username + "';";
            ResultSet rs = pstmt.executeQuery(query);
            //means the question hasnt been answered before so create new tuple with specified information
            if (!rs.next()) {
                pstmt = conn.prepareStatement("insert into intro10e"
                        + "(chapterNo, questionNo, isCorrect, time, hostname, answerA, answerB, answerC, answerD, answerE, username) values (?,?,?,CURRENT_TIMESTAMP,?,?,?,?,?,?,?)");

                //select host from information_schema.processlist WHERE ID=connection_id(); get IP address
                rs = pstmt.executeQuery("select host from information_schema.processlist WHERE ID=connection_id();");
                if (rs.next()) {
                    hostName = rs.getString(1);
                }
                pstmt.setInt(1, (chapterNumber + 1)); //chapterNo
                pstmt.setInt(2, questionNumber); //questionNo
                pstmt.setInt(3, isCorrect); //isCorrect
                pstmt.setString(4, hostName); //hostname 
                pstmt.setInt(5, answerA); //answerA
                pstmt.setInt(6, answerB); //answerB
                pstmt.setInt(7, answerC); //answerC
                pstmt.setInt(8, answerD); //answerD
                pstmt.setInt(9, answerE); //answerE
                pstmt.setString(10, username); //username
                pstmt.executeUpdate();
            } else { // ELSE just need to update the current tuple with new values
                rs = pstmt.executeQuery("select host from information_schema.processlist WHERE ID=connection_id();");
                if (rs.next()) {
                    hostName = rs.getString(1);
                }
                pstmt = conn.prepareStatement("Update intro10e set isCorrect=?, hostname=?, time=CURRENT_TIMESTAMP, answerA=?, answerB=?, answerC=?, answerD=?, answerE=? where chapterNo=? and questionNo=? and username=?");
                pstmt.setInt(8, (chapterNumber + 1)); //chapterNo
                pstmt.setInt(9, questionNumber); //questionNo
                pstmt.setInt(1, isCorrect); //isCorrect
                pstmt.setString(2, hostName); //hostname
                pstmt.setInt(3, answerA); //answerA
                pstmt.setInt(4, answerB); //answerB
                pstmt.setInt(5, answerC); //answerC
                pstmt.setInt(6, answerD); //answerD
                pstmt.setInt(7, answerE); //answerE
                pstmt.setString(10, username); //username
                pstmt.executeUpdate();
            }
        } catch (SQLException ex) {
        }
    }

    public ArrayList<String> getChapterList() {
        return new ArrayList<>(chapterList);
    }

    public ArrayList<String> getAllCheckedQuestions() {
        return new ArrayList<>(allCheckedQuestionsList);
    }
}