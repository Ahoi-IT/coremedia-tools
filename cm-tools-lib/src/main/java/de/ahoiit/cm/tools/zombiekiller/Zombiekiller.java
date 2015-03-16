package de.ahoiit.cm.tools.zombiekiller;
/*
 * Created by: alexander
 * $Id$
 */

import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.Version;
import com.coremedia.cmdline.AbstractUAPIClient;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>TODO Document!</p>
 */
@SuppressWarnings("deprecation")
public class Zombiekiller extends AbstractUAPIClient {

  private static final String PARAM_ID_SHORT = "i";
  private static final String PARAM_ID_LONG = "id";
  private static final String PARAM_QUERY_SHORT = "q";
  private static final String PARAM_QUERY_LONG = "query";
  private static final String PARAM_KILL_SHORT = "k";
  private static final String PARAM_KILL_LONG = "kill";


  private Integer id;

  private String query;

  private boolean kill;

  private Set<Content> seenContent = new HashSet<>(100);
  private Set<Version> versionsToBeKilled = new HashSet<>(10);
  private Set<Content> contentToBeDeleted = new HashSet<>(10);

  @Override
  @SuppressWarnings("static-access")
  protected void fillInOptions(Options options) {
    options.addOption(OptionBuilder.hasArg()
            .withDescription("Query to be executed. Either provide query or id!")
            .withLongOpt(PARAM_QUERY_LONG)
            .create(PARAM_QUERY_SHORT));
    options.addOption(OptionBuilder.hasArg()
            .withDescription("ID of the zombie element. Either provide query or id!")
            .withLongOpt(PARAM_ID_LONG)
            .create(PARAM_ID_SHORT));
    options.addOption(OptionBuilder
            .withDescription("do not just find zombies, but kill the referencing versions if possible")
            .withLongOpt(PARAM_KILL_LONG)
            .create(PARAM_KILL_SHORT));
  }

  @Override
  protected String getUsage() {
    return "cm zombie-killer -u <user> [other options] [--id <id> | --query <query>] [--kill]";
  }

  @Override
  protected boolean parseCommandLine(CommandLine commandLine) {
    kill = commandLine.hasOption(PARAM_KILL_SHORT) || commandLine.hasOption(PARAM_KILL_LONG);
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
      getContentRepository().getQueryService().checkQuery(query);

      final Collection<Content> contents;
      contents = this.getContentRepository().getQueryService().poseContentQuery(query);
      for (Content content : contents) {
        find(content);
      }
    }

    finish();
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
          if (referrer.getVersions().size() == 1) {
            if (!contentToBeDeleted.contains(referrer)) {
              getOut().warn("version " + referringVersion.getId() + " is only version of its content. " +
                      "Will have to delete content instead of kill version. " +
                      "You may need to run the tool again.");
              contentToBeDeleted.add(referrer);
            }
          } else {
            if (!versionsToBeKilled.contains(referringVersion)) {
              versionsToBeKilled.add(referringVersion);
            }
          }
        }
      }
    }
  }

  private void finish() {
    getOut().info("Zombie killer has looked at " + seenContent.size() + " deleted content items and found " +
            versionsToBeKilled.size() + " versions" +
            ((contentToBeDeleted.isEmpty()) ? "" : " and " + contentToBeDeleted.size() + " contents") + '.');
    for (Content content : contentToBeDeleted) {
      getOut().info(debugContent(content) + " references deleted content");
      if (kill) {
        getOut().info("deleting content " + content.getId() + "...");
        content.delete();
      }
    }
    for (Version version : versionsToBeKilled) {
      Content content = version.getContainingContent();
      getOut().info(debugContent(content) + " references deleted content in Version " + version.getId());
      if (kill) {
        getOut().info("deleting version " + version.getId() + "...");
        version.destroy();
      }
    }
  }

  private String debugContent(Content content) {
    return "Content \"" + content.getPath() + "\" (TYPE: " + content.getType().getName() + "; ID: " + content.getId() + ")";
  }
}
