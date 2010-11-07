/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Globally bound variables:
 *
 * log (org.slf4j.Logger)
 * repositories (org.artifactory.repo.Repositories repositories)
 * security (org.artifactory.security.Security security)
 *
 * context (org.artifactory.spring.InternalArtifactoryContext) - NOT A PUBLIC API - FOR INTERNAL USE ONLY!
 *
 * @author Yoav Landman
 */

/**
 * A section for handling and manipulating download events.
 */
download {

  /**
   * Provide an alternative response, by setting a success/error status code value and an optional error message.
   * Will not be called if the response is already committed (e.g. a previous error occurred).
   * Currently called only for GET requests where the resource was found.
   *
   * Context variables:
   * status (int) - a response status code. Defaults to -1 (unset).
   * message (java.lang.String) - a text message to return in the response body, replacing the response content.
   *                              Defaults to null.
   *
   * Closure parameters:
   * repoPath (org.artifactory.repo.RepoPath) - a read-only parameter of the original request RepoPath.
   */
  altResponse { repoPath ->
  }

  /**
   * Provides an alternative download path under the same remote repository, by setting a new value to the path
   * variable.
   *
   * Context variables:
   * path (java.lang.String) - the new path value. Defaults to the originalRepoPath's path.
   *
   * Closure parameters:
   * repoPath (org.artifactory.repo.RepoPath) - a read-only parameter of the original request RepoPath.
   */
  altRemotePath { repoPath ->
  }

  /**
   * Provide an alternative download content, by setting new values for the inputStream and size variables.
   * context variable.
   *
   * Context variables:
   * inputStream (java.io.InputStream) - a new stream that provides the response content. Defaults to null.
   * size (long) - the size of the new content (helpful for clients processing the response). Defaults to -1.
   *
   * Closure parameters:
   * repoPath (org.artifactory.repo.RepoPath) - a read-only parameter of the original request RepoPath.
   */
  altRemoteContent { repoPath ->
  }

  /**
   * Handle before remote download events.
   *
   * Closure parameters:
   * repoPath (org.artifactory.repo.RepoPath) - a read-only parameter of the original request RepoPath.
   */
  beforeRemoteDownload { repoPath ->
  }

  /**
   * Handle after remote download events.
   *
   * Closure parameters:
   * repoPath (org.artifactory.repo.RepoPath) - a read-only parameter of the original request RepoPath.
   */
  afterRemoteDownload { repoPath ->
  }
}

/**
 * A section for handling and manipulating storage events.
 *
 * If you wish to abort an action you can do that in 'before' methods by throwing a runtime
 * org.artifactory.exception.CancelException with an error message and a proper http error code.
 */
storage {

  /**
   * Handle before create events.
   *
   * Closure parameters:
   * item (org.artifactory.fs.ItemInfo) - the original item being created.
   */
  beforeCreate { item ->
  }

  /**
   * Handle after create events.
   *
   * Closure parameters:
   * item (org.artifactory.fs.ItemInfo) - the original item being created.
   */
  afterCreate { item ->
  }

  /**
   * Handle before create events.
   *
   * Closure parameters:
   * item (org.artifactory.fs.ItemInfo) - the original item being being deleted.
   */
  beforeDelete { item ->
  }

  /**
   * Handle after create events.
   *
   * Closure parameters:
   * item (org.artifactory.fs.ItemInfo) - the original item deleted.
   */
  afterDelete { item ->
  }

  /**
   * Handle before move events.
   *
   * Closure parameters:

   * item (org.artifactory.fs.ItemInfo) - the source item being moved.
   * targetRepoPath (org.artifactory.repo.RepoPath) - the target repoPath for the move.
   */
  beforeMove { item, targetRepoPath ->
  }

  /**
   * Handle after move events.
   *
   * Closure parameters:
   * item (org.artifactory.fs.ItemInfo) - the source item moved.
   * targetRepoPath (org.artifactory.repo.RepoPath) - the target repoPath for the move.
   */
  afterMove { item, targetRepoPath ->
  }

  /**
   * Handle before copy events.
   *
   * Closure parameters:
   * item (org.artifactory.fs.ItemInfo) - the source item being copied.
   * targetRepoPath (org.artifactory.repo.RepoPath) - the target repoPath for the copy.
   */
  beforeCopy { item, targetRepoPath ->
  }

  /**
   * Handle after copy events.
   *
   * Closure parameters:
   * item (org.artifactory.fs.ItemInfo) - the source item copied.
   * targetRepoPath (org.artifactory.repo.RepoPath) - the target repoPath for the copy.
   */
  afterCopy { item, targetRepoPath ->
  }
}

/**
 * A section for defining jobs.
 */
jobs {

  /**
   * A job definition.
   * The first value is a unique name for the job.
   * Job runs are controlled by the provided interval or cron expression, which are mutually exclusive.
   * The actual code to run as part of the job should be part of the job's closure.
   *
   * Parameters:
   * delay (long) - An initial delay in milliseconds before the job starts running (not applicable for a cron job).
   * interval (long) -  An interval in milliseconds between job runs.
   * cron (java.lang.String) - A valid cron expression used to schedule job runs (see: http://www.quartz-scheduler.org/docs/tutorial/TutorialLesson06.html)
   */

  /*
  myJob(interval: 1000, delay: 100) {
  }

  mySecondJob(cron: "0/1 * * * * ?") {
  }
  */
}

/**
 * A section for defining external executions.
 * External executions are invoked via REST POST requests. For example:
 * curl -X POST -v -u admin:password "http://localhost:8080/artifactory/api/plugins/execute/multiply?params=msg=And+the+result+is:|no1=10|no2=15&async=0"
 *
 * Since:  2.3.1
 */
executions {

  /**
   * An execution definition.
   * The first value is a unique name for the execution.
   *
   * Context variables:
   * status (int) - a response status code. Defaults to -1 (unset). Not applicable for an async execution.
   * message (java.lang.String) - a text message to return in the response body, replacing the response content.
   *                              Defaults to null. Not applicable for an async execution.
   *
   *  Closure parameters:
   * params (java.util.Map) - An execution takes a read-only key-value map that corresponds to the REST request
   * parameter 'params'. Each entry in the map conatains an array of values.
   */

  /*
  myExecution() { params ->
  }
  */
}
