/*
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

initscript {
  /*
  repositories {
    mavenRepo name: '${plugins.resolver.name}', urls: "${plugins.repository.url}"
  }
  dependencies {
    classpath 'XXX'
  }
  */
}

logger.debug("Applying Artifactory Gradle Settings")

addListener(new ArtifactoryGradleSettings())
class ArtifactoryGradleSettings extends BuildAdapter implements BuildListener {

  def void projectsLoaded(Gradle gradle) {
    Project root = gradle.getRootProject()
    root.allprojects {

      buildscript {
        repositories {
          mavenRepo name: '${plugins.resolver.name}', urls: "${plugins.repository.url}"
        }
      }

      repositories {
        mavenRepo name: '${libs.resolver.name}', urls: "${libs.repository.url}"
      }
    }
  }
}
