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
package org.sonar.server.computation.scm;

import com.google.common.base.Optional;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.db.source.FileSourceDto;
import org.sonar.server.computation.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.source.DbFileSourceFetcher;
import org.sonar.server.computation.source.DbFileSourceFetcherListener;
import org.sonar.server.computation.source.SourceHashRepository;

import static com.google.common.base.Preconditions.checkNotNull;

public class ScmInfoRepositoryImpl implements ScmInfoRepository, DbFileSourceFetcherListener {

  private static final Logger LOGGER = Loggers.get(ScmInfoRepositoryImpl.class);

  private final BatchReportReader batchReportReader;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final DbFileSourceFetcher fileSourceFetcher;
  private final SourceHashRepository sourceHashRepository;

  private final Map<Component, ScmInfo> scmInfoCache = new HashMap<>();

  public ScmInfoRepositoryImpl(BatchReportReader batchReportReader, AnalysisMetadataHolder analysisMetadataHolder, DbFileSourceFetcher fileSourceFetcher, SourceHashRepository sourceHashRepository) {
    this.batchReportReader = batchReportReader;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.fileSourceFetcher = fileSourceFetcher;
    this.sourceHashRepository = sourceHashRepository;
    // register itself to the DbFileSourceFetcher
    this.fileSourceFetcher.register(this);
  }

  @Override
  public void success(@Nonnull Component file, @Nonnull FileSourceDto dto) {
    if (sourceHashRepository.getRawSourceHash(file).equals(dto.getSrcHash())) {
      Optional<ScmInfo> scmInfo = DbScmInfo.create(file, dto.getSourceData().getLinesList());
      scmInfoCache.put(file, scmInfo.orNull());
    } else {
      scmInfoCache.put(file, null);
    }
  }

  @Override
  public void failed(@Nonnull Component file) {
    scmInfoCache.put(file, null);
  }

  @Override
  public Optional<ScmInfo> getScmInfo(Component component) {
    checkNotNull(component, "Component cannot be bull");
    initializeScmInfoForComponent(component);
    return Optional.fromNullable(scmInfoCache.get(component));
  }

  private void initializeScmInfoForComponent(Component component) {
    if (scmInfoCache.containsKey(component) || component.getType() != Component.Type.FILE) {
      return;
    }

    BatchReport.Changesets changesets = batchReportReader.readChangesets(component.getReportAttributes().getRef());
    if (changesets == null) {
      initializeScmInfoFromDb(component);
    } else {
      initializeScmInfoFromReport(component, changesets);
    }
  }

  /**
   * Retrieve file source from DB and add ScmInfo to scmInfoCache depending on whether fetch was successful or not
   * in methods {@link #success(Component, FileSourceDto)} and {@link #failed(Component)}.
   */
  private void initializeScmInfoFromDb(Component file) {
    if (!analysisMetadataHolder.isFirstAnalysis()) {
      fileSourceFetcher.fetch(file);
    }
  }

  private void initializeScmInfoFromReport(Component file, BatchReport.Changesets changesets) {
    LOGGER.trace("Reading SCM info from report for file '{}'", file.getKey());
    ReportScmInfo scmInfo = new ReportScmInfo(changesets);
    scmInfoCache.put(file, scmInfo);
  }
}
