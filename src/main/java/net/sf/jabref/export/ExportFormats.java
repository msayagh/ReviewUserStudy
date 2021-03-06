/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.export;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.*;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import net.sf.jabref.*;
import net.sf.jabref.plugin.PluginCore;
import net.sf.jabref.plugin.core.JabRefPlugin;
import net.sf.jabref.plugin.core.generated._JabRefPlugin.ExportFormatExtension;
import net.sf.jabref.plugin.core.generated._JabRefPlugin.ExportFormatProviderExtension;
import net.sf.jabref.plugin.core.generated._JabRefPlugin.ExportFormatTemplateExtension;

/**
 * User: alver
 * 
 * Date: Oct 18, 2006 
 * 
 * Time: 9:35:08 PM 
 */
public class ExportFormats {

    private static final Map<String, IExportFormat> exportFormats = new TreeMap<String, IExportFormat>();

    // Global variable that is used for counting output entries when exporting:
    public static int entryNumber = 0;


    public static void initAllExports() {

        ExportFormats.exportFormats.clear();

        // Initialize Build-In Export Formats
        ExportFormats.putFormat(new ExportFormat(
                Globals.lang("HTML"), "html", "html", null, ".html"));
        ExportFormats.putFormat(new ExportFormat(
                Globals.lang("Simple HTML"), "simplehtml", "simplehtml", null, ".html"));
        ExportFormats.putFormat(new ExportFormat(Globals.lang("DocBook").concat(" 4.4"), "docbook", "docbook", null, ".xml"));
        ExportFormats.putFormat(new ExportFormat(Globals.lang("DIN 1505"), "din1505", "din1505winword", "din1505", ".rtf"));
        ExportFormats.putFormat(new ExportFormat(Globals.lang("BibTeXML"), "bibtexml", "bibtexml", null, ".xml"));
        ExportFormats.putFormat(new ExportFormat(Globals.lang("BibO RDF"), "bibordf", "bibordf", null, ".rdf"));
        ExportFormats.putFormat(new ModsExportFormat());
        ExportFormats.putFormat(new ExportFormat(Globals.lang("HTML table"),
                "tablerefs", "tablerefs", "tablerefs", ".html"));
        ExportFormats.putFormat(new ExportFormat(Globals.lang("HTML list"),
                "listrefs", "listrefs", "listrefs", ".html"));
        ExportFormats.putFormat(new ExportFormat(Globals.lang("HTML table (with Abstract & BibTeX)"),
                "tablerefsabsbib", "tablerefsabsbib", "tablerefsabsbib", ".html"));
        ExportFormats.putFormat(new ExportFormat(Globals.lang("Harvard RTF"), "harvard", "harvard",
                "harvard", ".rtf"));
        ExportFormats.putFormat(new ExportFormat(Globals.lang("ISO 690"), "iso690rtf", "iso690RTF", "iso690rtf", ".rtf"));
        ExportFormats.putFormat(new ExportFormat(Globals.lang("ISO 690"), "iso690txt", "iso690", "iso690txt", ".txt"));
        ExportFormats.putFormat(new ExportFormat(Globals.lang("Endnote"), "endnote", "EndNote",
                "endnote", ".txt"));
        ExportFormats.putFormat(new ExportFormat(Globals.lang("OpenOffice CSV"), "oocsv", "openoffice-csv",
                "openoffice", ".csv"));
        ExportFormat ef = new ExportFormat(Globals.lang("RIS"), "ris", "ris", "ris", ".ris");
        ef.encoding = "UTF-8";
        ExportFormats.putFormat(ef);
        ExportFormats.putFormat(new OpenOfficeDocumentCreator());
        ExportFormats.putFormat(new OpenDocumentSpreadsheetCreator());
        ExportFormats.putFormat(new MSBibExportFormat());
        ExportFormats.putFormat(new MySQLExport());
        ExportFormats.putFormat(new PostgreSQLExport());

        // Add Export Formats contributed by Plugins
        JabRefPlugin plugin = JabRefPlugin.getInstance(PluginCore.getManager());
        if (plugin != null) {

            // 1. ExportFormats based on Templates
            for (ExportFormatTemplateExtension e : plugin.getExportFormatTemplateExtensions()) {
                ExportFormat format = PluginBasedExportFormat.getFormat(e);
                if (format != null) {
                    ExportFormats.putFormat(format);
                }
            }

            // 2. ExportFormat classed 
            for (final ExportFormatExtension e : plugin.getExportFormatExtensions()) {
                ExportFormats.putFormat(new IExportFormat() {

                    @Override
                    public String getConsoleName() {
                        return e.getConsoleName();
                    }

                    @Override
                    public String getDisplayName() {
                        return e.getDisplayName();
                    }

                    @Override
                    public FileFilter getFileFilter() {
                        return new ExportFileFilter(this, e.getExtension());
                    }


                    IExportFormat wrapped;


                    @Override
                    public void performExport(BibtexDatabase database, MetaData metaData,
                            String file, String encoding, Set<String> entryIds)
                            throws Exception {

                        if (wrapped == null) {
                            wrapped = e.getExportFormat();
                        }
                        wrapped.performExport(database, metaData, file, encoding, entryIds);
                    }
                });
            }

            // 3. Formatters provided by Export Format Providers
            for (ExportFormatProviderExtension e : plugin.getExportFormatProviderExtensions()) {
                IExportFormatProvider formatProvider = e.getFormatProvider();
                for (IExportFormat exportFormat : formatProvider.getExportFormats()) {
                    ExportFormats.putFormat(exportFormat);
                }
            }
        }

        // Now add custom export formats
        TreeMap<String, ExportFormat> customFormats = Globals.prefs.customExports.getCustomExportFormats();
        for (IExportFormat format : customFormats.values()) {
            ExportFormats.putFormat(format);
        }
    }

    /**
     * Build a string listing of all available export formats.
     * 
     * @param maxLineLength
     *            The max line length before a line break must be added.
     * @param linePrefix
     *            If a line break is added, this prefix will be inserted at the
     *            beginning of the next line.
     * @return The string describing available formats.
     */
    public static String getConsoleExportList(int maxLineLength, int firstLineSubtr,
            String linePrefix) {
        StringBuilder sb = new StringBuilder();
        int lastBreak = -firstLineSubtr;

        for (String name : ExportFormats.exportFormats.keySet()) {
            if (((sb.length() + 2 + name.length()) - lastBreak) > maxLineLength) {
                sb.append(",\n");
                lastBreak = sb.length();
                sb.append(linePrefix);
            } else if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(name);
        }

        return sb.toString();
    }

    /**
     * Get a Map of all export formats.
     * @return A Map containing all export formats, mapped to their console names.
     */
    public static Map<String, IExportFormat> getExportFormats() {
        // It is perhaps overly paranoid to make a defensive copy in this case:
        return Collections.unmodifiableMap(ExportFormats.exportFormats);
    }

    /**
     * Look up the named export format.
     * 
     * @param consoleName
     *            The export name given in the JabRef console help information.
     * @return The ExportFormat, or null if no exportformat with that name is
     *         registered.
     */
    public static IExportFormat getExportFormat(String consoleName) {
        return ExportFormats.exportFormats.get(consoleName);
    }

    /**
     * Create an AbstractAction for performing an export operation.
     * 
     * @param frame
     *            The JabRefFrame of this JabRef instance.
     * @param selectedOnly
     *            true indicates that only selected entries should be exported,
     *            false indicates that all entries should be exported.
     * @return The action.
     */
    public static AbstractAction getExportAction(JabRefFrame frame, boolean selectedOnly) {

        class ExportAction extends MnemonicAwareAction {

            private static final long serialVersionUID = 639463604530580554L;

            private final JabRefFrame frame;

            private final boolean selectedOnly;


            public ExportAction(JabRefFrame frame, boolean selectedOnly) {
                this.frame = frame;
                this.selectedOnly = selectedOnly;
                putValue(Action.NAME, selectedOnly ? "Export selected entries" : "Export");
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                ExportFormats.initAllExports();
                JFileChooser fc = ExportFormats.createExportFileChooser(
                        Globals.prefs.get(JabRefPreferences.EXPORT_WORKING_DIRECTORY));
                fc.showSaveDialog(frame);
                File file = fc.getSelectedFile();
                if (file == null) {
                    return;
                }
                FileFilter ff = fc.getFileFilter();
                if (ff instanceof ExportFileFilter) {

                    ExportFileFilter eff = (ExportFileFilter) ff;
                    String path = file.getPath();
                    if (!path.endsWith(eff.getExtension())) {
                        path = path + eff.getExtension();
                    }
                    file = new File(path);
                    if (file.exists()) {
                        // Warn that the file exists:
                        if (JOptionPane.showConfirmDialog(frame, '\'' + file.getName() + "' "
                                + Globals.lang("exists. Overwrite file?"), Globals.lang("Export"),
                                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                            return;
                        }
                    }
                    final IExportFormat format = eff.getExportFormat();
                    Set<String> entryIds = null;
                    if (selectedOnly) {
                        BibtexEntry[] selected = frame.basePanel().getSelectedEntries();
                        entryIds = new HashSet<String>();
                        for (BibtexEntry bibtexEntry : selected) {
                            entryIds.add(bibtexEntry.getId());
                        }
                    }

                    // Set the global variable for this database's file directory before exporting,
                    // so formatters can resolve linked files correctly.
                    // (This is an ugly hack!)
                    Globals.prefs.fileDirForDatabase = frame.basePanel().metaData()
                            .getFileDirectory(GUIGlobals.FILE_FIELD);
                    // Also store the database's file in a global variable:
                    Globals.prefs.databaseFile = frame.basePanel().metaData().getFile();

                    // Make sure we remember which filter was used, to set
                    // the default for next time:
                    Globals.prefs.put(JabRefPreferences.LAST_USED_EXPORT, format.getConsoleName());
                    Globals.prefs.put(JabRefPreferences.EXPORT_WORKING_DIRECTORY, file.getParent());

                    final File finFile = file;
                    final Set<String> finEntryIDs = entryIds;
                    AbstractWorker exportWorker = new AbstractWorker() {

                        String errorMessage = null;


                        @Override
                        public void run() {
                            try {
                                format.performExport(frame.basePanel().database(),
                                        frame.basePanel().metaData(),
                                        finFile.getPath(), frame
                                                .basePanel().getEncoding(), finEntryIDs);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                if (ex.getMessage() == null) {
                                    errorMessage = ex.toString();
                                } else {
                                    errorMessage = ex.getMessage();
                                }
                            }
                        }

                        @Override
                        public void update() {
                            // No error message. Report success:
                            if (errorMessage == null) {
                                frame.output(Globals.lang("%0 export successful", format.getDisplayName()));
                            }
                            // ... or show an error dialog:
                            else {
                                frame.output(Globals.lang("Could not save file")
                                        + " - " + errorMessage);
                                // Need to warn the user that saving failed!
                                JOptionPane.showMessageDialog(frame, Globals.lang("Could not save file")
                                        + ".\n" + errorMessage, Globals.lang("Save database"),
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    };

                    // Run the export action in a background thread:
                    (exportWorker.getWorker()).run();
                    // Run the update method:
                    exportWorker.update();
                }
            }
        }

        return new ExportAction(frame, selectedOnly);
    }

    private static JFileChooser createExportFileChooser(String currentDir) {
        String lastUsedFormat = Globals.prefs.get(JabRefPreferences.LAST_USED_EXPORT);
        FileFilter defaultFilter = null;
        JFileChooser fc = new JFileChooser(currentDir);
        TreeSet<FileFilter> filters = new TreeSet<FileFilter>();
        for (Map.Entry<String, IExportFormat> e : ExportFormats.exportFormats.entrySet()) {
            String formatName = e.getKey();
            IExportFormat format = e.getValue();
            filters.add(format.getFileFilter());
            if (formatName.equals(lastUsedFormat)) {
                defaultFilter = format.getFileFilter();
            }
        }
        for (FileFilter ff : filters) {
            fc.addChoosableFileFilter(ff);
        }
        fc.setAcceptAllFileFilterUsed(false);
        if (defaultFilter != null) {
            fc.setFileFilter(defaultFilter);
        }
        return fc;
    }

    private static void putFormat(IExportFormat format) {
        ExportFormats.exportFormats.put(format.getConsoleName(), format);
    }

}
