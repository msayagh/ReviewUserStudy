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
package net.sf.jabref.collab;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.*;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableInsertString;
import net.sf.jabref.undo.UndoableStringChange;

class StringNameChange extends Change {

    private static final long serialVersionUID = 1L;
    
    private final BibtexString string;
    private final String mem;
    private final String disk;
    private final String content;
    private final BibtexString tmpString;
    
    private static final Log LOGGER = LogFactory.getLog(StringNameChange.class);


    public StringNameChange(BibtexString string, BibtexString tmpString,
            String mem, String tmp, String disk, String content) {
        this.tmpString = tmpString;
        name = Globals.lang("Renamed string") + ": '" + tmp + '\'';
        this.string = string;
        this.content = content;
        this.mem = mem;
        this.disk = disk;

    }

    @Override
    public boolean makeChange(BasePanel panel, BibtexDatabase secondary, NamedCompound undoEdit) {

        if (panel.database().hasStringLabel(disk)) {
            // The name to change to is already in the database, so we can't comply.
            LOGGER.info("Cannot rename string '" + mem + "' to '" + disk + "' because the name "
                    + "is already in use.");
        }

        if (string != null) {
            string.setName(disk);
            undoEdit.addEdit(new UndoableStringChange(panel, string, true, mem,
                    disk));
        } else {
            // The string was removed or renamed locally. We guess that it was removed.
            String newId = IdGenerator.next();
            BibtexString bs = new BibtexString(newId, disk, content);
            try {
                panel.database().addString(bs);
                undoEdit.addEdit(new UndoableInsertString(panel, panel.database(), bs));
            } catch (KeyCollisionException ex) {
                LOGGER.info("Error: could not add string '" + bs.getName() + "': " + ex.getMessage(), ex);
            }
        }

        // Update tmp database:
        if (tmpString != null) {
            tmpString.setName(disk);
        }
        else {
            String newId = IdGenerator.next();
            BibtexString bs = new BibtexString(newId, disk, content);
            secondary.addString(bs);
        }

        return true;
    }

    @Override
    JComponent description() {
        return new JLabel(disk + " : " + content);
    }

}
