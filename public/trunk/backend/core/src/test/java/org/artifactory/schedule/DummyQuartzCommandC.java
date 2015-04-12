/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.schedule;

/**
 * User: freds
 * Date: 7/10/11
 * Time: 10:45 AM
 */
@JobCommand(
        schedulerUser = TaskUser.CURRENT,
        manualUser = TaskUser.CURRENT,
        keyAttributes = {DummyQuartzCommandC.TEST_KEY},
        commandsToStop = {
                @StopCommand(command = DummyQuartzCommandA.class, strategy = StopStrategy.IMPOSSIBLE)
        }
)
public class DummyQuartzCommandC extends DummyStopCancelQuartzCommand {
    public static final String TEST_KEY = "TEST_KEY";
}
