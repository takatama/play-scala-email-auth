package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import models.{User, Token}
import views._

trait Secured {
  //FIXME do not save email to cookie
  private def username(request: RequestHeader) = request.session.get("email")
  private def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Authentication.login).withSession("uri" -> request.uri)
  def SecureAction(f: => String => Request[AnyContent] => Result) = Security.Authenticated(username, onUnauthorized) { user =>
    Action(request => f(user)(request))
  }
}

object Authentication extends Controller {
  val loginForm = Form(
    tuple(
      "email" -> text,
      "password" -> text
    ) verifying ("Invalid email or password", result => result match {
      case (email, password) => User.authenticate(email, password).isDefined
    })
  )

  def login = Action { implicit request =>
    Ok(views.html.login(loginForm))
  }

  def authenticate = Action { implicit request => 
    val uri = session.get("uri").getOrElse("/")
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.login(formWithErrors)),
      //FIXME do not save email to cookie
      user => Redirect(uri).withSession("email" -> user._1)
    )
  }

  def logout = Action {
    Redirect(routes.Authentication.login).withNewSession.flashing(
      "success" -> "You've been logged out"
    )
  }

  val prepareForm = Form(
    single(
      "email" -> nonEmptyText
    ) verifying ("The email has been registered", result => result match {
      case(email) => User.findByEmail(email).isEmpty
    })
  )

  def prepareSignup = Action { implicit request =>
    Ok(views.html.signup(prepareForm))
  }
  
  import play.api.Play.current
  import com.typesafe.plugin._

  private def sendEmail(toAddress: String, subject: String, body: String) {
    val mail =  use[MailerPlugin].email
    mail.addFrom(Play.current.configuration.getString("smtp.user").getOrElse(""))
    mail.addRecipient(toAddress)
    mail.setSubject(subject)
    mail.send(body)
  }

  def sendSignupEmail = Action { implicit request =>
    prepareForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.signup(formWithErrors)),
      email => {
        val token = Token.create(email)
	val host = Play.current.configuration.getString("host").getOrElse("http://localhost:9000")
	sendEmail(email, "Welcome to Garoku", host + routes.Authentication.prepareSignup + "/" + token.token)
        Ok(views.html.send(Token.all()))
      }
    )
  }

  val registerForm = Form(
    tuple(
      "name" -> nonEmptyText,
      "password" -> nonEmptyText
    )
  )

  private def redirectToSignup = {
    Redirect(routes.Authentication.prepareSignup).withNewSession.flashing(
      "error" -> "Signup url is invalid. Please retry to send email"
    )
  }

  def prepareRegister(token: String) = Action { implicit request =>
    Token.findByToken(token) match {
      case Some(token) => {
        Ok(views.html.register(registerForm, token))
      }
      case None => redirectToSignup
    }
  }

  def register(token: String) = Action { implicit request =>
    Token.findByToken(token) match {
      case Some(token) => {
        registerForm.bindFromRequest.fold(
	  errors => redirectToSignup,
	  form => {
            User.create(token.email, form._1, form._2)
	    //FIXME customize top page
	    //FIXME do not save email to cookie
            Redirect(routes.Application.index).withSession("email" -> token.email)
	  }
	)
      }
      case None => redirectToSignup
    }
  }
}
