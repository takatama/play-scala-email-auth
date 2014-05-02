package models

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class User(id: Pk[Long], email: String, name: String, password: String)

object User {
  val simple = {
    get[Pk[Long]]("user.id") ~
    get[String]("user.email") ~
    get[String]("user.name") ~
    get[String]("user.password") map {
      case id~email~name~password => User(id, email, name, password)
    }
  }

  def create(user: User): User = {
    DB.withConnection { implicit connection =>

      val id: Long = user.id.getOrElse {
        SQL("select next value for user_seq").as(scalar[Long].single)
      }

      SQL(
        """
	  insert into user values (
	    {id}, {email}, {name}, {password}
	  )
	"""
      ).on(
        "id" -> id,
	"email" -> user.email,
	"name" -> user.name,
	"password" -> user.password
      ).executeUpdate()

      user.copy(id = Id(id))
    }
  }

  def authenticate(email: String, password: String): Option[User] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
	  select * from user where
	  email = {email} and password = {password}
	"""
      ).on(
        "email" -> email,
	"password" -> password
      ).as(User.simple.singleOpt)
    }
  }
}
