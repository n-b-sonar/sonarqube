/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.source;

import java.util.HashSet;
import java.util.Set;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.source.FileSourceDto;
import org.sonar.server.computation.component.Component;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class DbFileSourceFetcherImpl implements DbFileSourceFetcher {
  private static final Logger LOGGER = Loggers.get(DbFileSourceFetcherImpl.class);

  private final DbClient dbClient;
  // initialize to size 2 because we expect to have ScmInfoRepositoryImpl and
  // at most one instance of TrackerBaseInputFactory.SourceFetcherListener at a time
  private final Set<DbFileSourceFetcherListener> listeners = new HashSet<>(2);

  public DbFileSourceFetcherImpl(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void register(DbFileSourceFetcherListener listener) {
    requireNonNull(listener, "listener can not be null");

    listeners.add(listener);
  }

  @Override
  public void unregister(DbFileSourceFetcherListener listener) {
    requireNonNull(listener, "listener can not be null");

    listeners.remove(listener);
  }

  @Override
  public void fetch(Component file) {
    requireNonNull(file, "file can not be null");
    checkArgument(file.getType() == Component.Type.FILE, "file must be of type FILE");

    LOGGER.debug("Fetching source for file {}", file);
    DbSession dbSession = dbClient.openSession(false);
    try {
      FileSourceDto dto = dbClient.fileSourceDao().selectSourceByFileUuid(dbSession, file.getUuid());
      if (dto == null) {
        notifyFailure(file);
      } else {
        notifySuccess(file, dto);
      }
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private void notifyFailure(Component file) {
    LOGGER.debug("Notifying fetch failure for file {}", file);
    for (DbFileSourceFetcherListener listener : listeners) {
      listener.failed(file);
    }
  }

  private void notifySuccess(Component file, FileSourceDto dto) {
    LOGGER.debug("Notifying successful fetch for file {}: {}", file, dto);
    for (DbFileSourceFetcherListener listener : listeners) {
      listener.success(file, dto);
    }
  }
}
