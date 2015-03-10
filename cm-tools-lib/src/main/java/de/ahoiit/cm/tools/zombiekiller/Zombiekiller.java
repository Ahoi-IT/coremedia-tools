package de.ahoiit.cm.tools.zombiekiller;
/*
 * Created by: alexander
 * $Id$
 */

import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.Version;
import com.coremedia.cap.content.query.MalformedQueryException;
import com.coremedia.cmdline.AbstractUAPIClient;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import java.util.*;

/**
 * <p>TODO Document!</p>
 */
public class Zombiekiller extends AbstractUAPIClient {

  private static final String PARAM_ID_SHORT = "i";
  private static final String PARAM_ID_LONG = "id";
  private static final String PARAM_QUERY_SHORT = "q";
  private static final String PARAM_QUERY_LONG = "query";

  private Integer id;

  private String query;

  private Set<Content> seenContent = new HashSet<>(100);
  private Set<Content> result = new HashSet<>(10);

  @Override
  protected void fillInOptions(Options options) {
    options.addOption(OptionBuilder.hasArg()
            .withDescription("Query to be executed. Either provide query or id!")
            .withLongOpt(PARAM_QUERY_LONG)
            .create(PARAM_QUERY_SHORT));
    options.addOption(OptionBuilder.hasArg()
            .withDescription("ID of the zombie element. Either provide query or id!")
            .withLongOpt(PARAM_ID_LONG)
            .create(PARAM_ID_SHORT));
  }

  @Override
  protected String getUsage() {
    return "cm zombiekiller -u <user> [other options] [--id <id> | --query <query>]";
  }

  @Override
  protected boolean parseCommandLine(CommandLine commandLine) {
    query = commandLine.getOptionValue(PARAM_QUERY_SHORT);
    String id_ = commandLine.getOptionValue(PARAM_ID_SHORT);

    if (id_ != null) {
      try {
        id = Integer.valueOf(id_);
      } catch (NumberFormatException e) {
        getOut().error("id " + id_ + " is not a number");
        return false;
      }
    }

    if (query != null) {
      try {
        getContentRepository().getQueryService().checkQuery(query);
      } catch (MalformedQueryException e) {
        getOut().error("malformed query");
        return false;
      }
    }

    if (id == null && query == null) {
      getOut().error("neither 'id' nor 'query' given - specify one.");
      return false;
    }

    if (id != null && query != null) {
      getOut().error("both 'id' and 'query' given - choose one.");
      return false;
    }

    return true;
  }

  @Override
  protected void run() throws Exception {
    if (id != null) {
      final Content content = getContentRepository().getContent(String.valueOf(id));
      find(content);
    } else {
      final Collection<Content> contents;
      contents = this.getContentRepository().getQueryService().poseContentQuery(query);
      for (Content content : contents) {
        find(content);
      }
    }

    printResult();
  }

  public static void main(String[] args) {
    main(new Zombiekiller(), args);
  }


  private void find(Content content) {
    for (Version referringVersion : ((com.coremedia.cap.undoc.content.Content)content).getReferringVersions()) {
      Content referrer=referringVersion.getContainingContent();
      if (!seenContent.contains(referrer)) {
        seenContent.add(referrer);
        if (referrer.isDeleted()) {
          find(referrer); //enter recursion
        } else {
          if (!result.contains(referrer)) {
            result.add(referrer);
          }
        }
      }
    }
  }

  private void printResult() {
    getOut().info("Zombie killer has looked at " + seenContent.size() + " deleted content items and found " +
            result.size() + " undeleted content items referring to it:");
    for (Content content : result) {
      getOut().info("- " + content.getId() + " (" + content.getType().getName() + " / " + content.getPath() + ")");
    }
  }
}
