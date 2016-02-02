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
package org.sonar.server.computation.issue;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Input;
import org.sonar.core.issue.tracking.LazyInput;
import org.sonar.core.issue.tracking.LineHashSequence;
import org.sonar.db.source.FileSourceDao;
import org.sonar.db.source.FileSourceDto;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.source.DbFileSourceFetcher;
import org.sonar.server.computation.source.DbFileSourceFetcherListener;

/**
 * Factory of {@link Input} of base data for issue tracking. Data are lazy-loaded.
 */
public class TrackerBaseInputFactory {

  private final BaseIssuesLoader baseIssuesLoader;
  private final DbFileSourceFetcher fileSourceFetcher;

  public TrackerBaseInputFactory(BaseIssuesLoader baseIssuesLoader, DbFileSourceFetcher fileSourceFetcher) {
    this.baseIssuesLoader = baseIssuesLoader;
    this.fileSourceFetcher = fileSourceFetcher;
  }

  public Input<DefaultIssue> create(Component component) {
    return new BaseLazyInput(component);
  }

  private class BaseLazyInput extends LazyInput<DefaultIssue> {
    private final Component component;

    private BaseLazyInput(Component component) {
      this.component = component;
    }

    @Override
    protected LineHashSequence loadLineHashSequence() {
      SourceFetcherListener listener = new SourceFetcherListener();
      try {
        fileSourceFetcher.register(listener);
        fileSourceFetcher.fetch(component);
      } finally {
        fileSourceFetcher.unregister(listener);
      }

      return new LineHashSequence(listener.dto == null ? Collections.<String>emptyList() : FileSourceDao.parseLineHashes(listener.dto));
    }

    @Override
    protected List<DefaultIssue> loadIssues() {
      return baseIssuesLoader.loadForComponentUuid(component.getUuid());
    }

    private class SourceFetcherListener implements DbFileSourceFetcherListener {
      private FileSourceDto dto;

      @Override
      public void success(@Nonnull Component file, @Nonnull FileSourceDto dto) {
        this.dto = dto;
      }

      @Override
      public void failed(@Nonnull Component file) {
        // do nothing
      }
    }
  }
}
