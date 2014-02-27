package services

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.ws.WS

import models.JiraIssue

object JiraApi extends utils.Config {
  lazy val apiUrl = jiraUrl + "/rest/api/2"
  lazy val apiUrlIssues = apiUrl + "/issue"

  lazy val api = WS.url(jiraUrl + "/rest/api/2")//.withHeaders("Authorization" -> ("Basic " + jiraAuthBasic))
  lazy val apiIssues = WS.url(apiUrlIssues)//.withHeaders("Authorization" -> ("Basic " + jiraAuthBasic))

  def issueUrl(key: String) = jiraUrl + "/browse/" + key

  def get(key: String): Future[JiraIssue] =
    WS.url(apiUrlIssues + "/" + key)
      .withHeaders("Authorization" -> ("Basic " + jiraAuthBasic))
      .get.map(_.json.as[JiraIssue])

  def create(summary: String, description: String, project: String, issueType: String): Future[JsObject] = {
    val projectLabel = if (utils.Numbers.isAllDigits(project)) { "id" } else { "key" }
    val issueTypeLabel = if (utils.Numbers.isAllDigits(issueType)) { "id" } else { "name" }

    val newIssue = Json.obj(
      "fields" -> Json.obj(
        "project" -> Json.obj(projectLabel -> project),
        "summary" -> summary,
        "description" -> description,
        "issuetype" -> Json.obj(issueTypeLabel -> issueType)
      )
    )

    apiIssues.post(newIssue).map(_.json.as[JsObject])
  }
}