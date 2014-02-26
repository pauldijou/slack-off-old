package models

import play.api.libs.json._
import play.api.libs.functional.syntax._

// USER
case class JiraUser(
  self: String,
  name: String,
  emailAddress: String,
  avatarUrls: Map[String, String],
  displayName: String,
  active: Boolean
)

object JiraUser {
  implicit val jiraUserFormat = Json.format[JiraUser]
}

// PRIORITY
case class JiraPriority(self: String, iconUrl: String, name: String, id: String)
object JiraPriority { implicit val jiraPriorityFormat = Json.format[JiraPriority] }

// PROJECT CATEGORY
case class JiraProjectCategory(self: String, id: String, description: String, name: String)
object JiraProjectCategory { implicit val jiraProjectCategoryFormat = Json.format[JiraProjectCategory] }

// PROJECT
case class JiraProject(self: String, id: String, key: String, name: String, avatarUrls: Map[String, String], projectCategory: JiraProjectCategory)
object JiraProject { implicit val jiraProjectFormat = Json.format[JiraProject] }

// COMMENT
case class JiraComment(self: String, id: String, author: JiraUser, body: String, updateAuthor: JiraUser, created: String, updated: String)
object JiraComment { implicit val jiraCommentFormat = Json.format[JiraComment] }

// COMMENTS
case class JiraComments(startAt: Long, maxResults: Long, total: Long, comments: List[JiraComment])
object JiraComments { implicit val jiraCommentsFormat = Json.format[JiraComments] }

// ISSUE TYPE
case class JiraIssueType(self: String, id: String, description: String, iconUrl: String, name: String, subtask: Boolean)
object JiraIssueType { implicit val jiraIssueTypeFormat = Json.format[JiraIssueType] }

// FIELDS
case class JiraIssueFields(
  summary: String,
  progress: Map[String, Long],
  issuetype: JiraIssueType,
  reporter: Option[JiraUser],
  updated: String,
  created: String,
  description: String,
  priority: JiraPriority,
  project: JiraProject,
  comment: JiraComments,
  creator: JiraUser,
  assignee: Option[JiraUser]
)

object JiraIssueFields { implicit val jiraIssueFieldsFormat = Json.format[JiraIssueFields] }

// ISSUE
case class JiraIssue(
  id: String,
  self: String,
  key: String,
  fields: JiraIssueFields
)

object JiraIssue {
  implicit val jiraIssueFormat = Json.format[JiraIssue]
}

// WEBHOOK ACTION
object JiraWebhookAction extends Enumeration {
  type JiraWebhookAction = Value
  val ISSUE_CREATED, ISSUE_UPDATED, ISSUE_DELETED, WORKLOG_UPDATED = Value;

  implicit def JiraWebhookActionFormat = new Format[JiraWebhookAction.Value] {
    def reads(js: JsValue) = js match {
      case JsString("jira:issue_created") => JsSuccess(ISSUE_CREATED)
      case JsString("jira:issue_updated") => JsSuccess(ISSUE_UPDATED)
      case JsString("jira:issue_deleted") => JsSuccess(ISSUE_DELETED)
      case JsString("jira:worklog_updated") => JsSuccess(WORKLOG_UPDATED)
      case _ => JsError("Wrong JiraWebhookAction name")
    }

    def writes(p: JiraWebhookAction.Value) = JsString(p.toString())
  }
}


// WEBHOOK EVENT
case class JiraWebhookEvent (
  webhookEvent: JiraWebhookAction.Value,
  user: JiraUser,
  issue: JiraIssue,
  comment: Option[JiraComment],
  timestamp: Long
) {
  lazy val created = (this.webhookEvent == JiraWebhookAction.ISSUE_CREATED)
  lazy val updated = (this.webhookEvent == JiraWebhookAction.ISSUE_UPDATED)
  lazy val deleted = (this.webhookEvent == JiraWebhookAction.ISSUE_DELETED)
  lazy val worklogUpdated = (this.webhookEvent == JiraWebhookAction.WORKLOG_UPDATED)

  lazy val isComment = updated && (this.comment.map(c => true) getOrElse false)
  lazy val isNewComment = updated && (this.comment.map(c => c.created == c.updated) getOrElse false)
  lazy val isUpdatedComment = updated && (this.comment.map(c => c.created != c.updated) getOrElse false)
}

object JiraWebhookEvent {
  implicit val jiraWebhookEventFormat = Json.format[JiraWebhookEvent]
}