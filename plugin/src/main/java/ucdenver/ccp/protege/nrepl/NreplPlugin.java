/*
 * The contents of this file are subject to the LGPL License, Version 3.0.
 *
 * Copyright (C) 2013, Phillip Lord, Newcastle University
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package ucdenver.ccp.protege.nrepl;

import clojure.lang.DynamicClassLoader;
import clojure.lang.RT;
import clojure.lang.Var;
import org.protege.editor.owl.ui.action.ProtegeOWLAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;


public class NreplPlugin extends ProtegeOWLAction {
    private static final long serialVersionUID = -2896209622461162777L;
    private static Object lock = new Object();
    private static boolean clojureInit = false;

    private final ClassLoader cl = new DynamicClassLoader(this.getClass().getClassLoader());

    public void initialise() throws Exception {
    }

    public void dispose() throws Exception {
    }

    public static void main(String[] args) {
    }

    public void actionPerformed(ActionEvent event) {
        Thread.currentThread().
                setContextClassLoader(cl);

        final JFrame frame = new JFrame("REPL connect");
        final Container cp = frame.getContentPane();
        final JLabel initLabel = new JLabel("Initializing Clojure");

        cp.add(initLabel);
        frame.setSize(200,100);
        frame.setVisible(true);
        frame.validate();
        frame.repaint();

        final Runnable after = new Runnable(){
            public void run(){
                Thread.currentThread().setContextClassLoader(cl);
                Var newDialog = RT.var("protege-nrepl.dialog", "new-dialog-panel");
                cp.removeAll();
                JPanel dialog = (JPanel)newDialog. invoke(getOWLEditorKit());

                cp.add(dialog);
                frame.pack();
                frame.setVisible(true);
                frame.validate();
                frame.repaint();

            }
        };

        final Runnable before = new Runnable(){
            public void run(){
                Thread.currentThread().setContextClassLoader(cl);
                try{
                    if(!clojureInit){
                        synchronized(lock){
                            RT.loadResourceScript("protege_nrepl/dialog.clj");
                            RT.loadResourceScript("protege_nrepl/core.clj");
                            initLabel.setText("Reading User Init");
                            Var init = RT.var("protege-nrepl.core","init");
                            init.invoke();
                            clojureInit=true;
                        }
                    }
                    SwingUtilities.invokeAndWait(after);

                }
                catch(Exception exp){
                    throw new RuntimeException(exp);
                }
            }
        };

        new Thread(before).start();
    }
}
