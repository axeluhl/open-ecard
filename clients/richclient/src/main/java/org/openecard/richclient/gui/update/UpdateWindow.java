/****************************************************************************
 * Copyright (C) 2018 ecsec GmbH.
 * All rights reserved.
 * Contact: ecsec GmbH (info@ecsec.de)
 *
 * This file is part of the Open eCard App.
 *
 * GNU General Public License Usage
 * This file may be used under the terms of the GNU General Public
 * License version 3.0 as published by the Free Software Foundation
 * and appearing in the file LICENSE.GPL included in the packaging of
 * this file. Please review the following information to ensure the
 * GNU General Public License version 3.0 requirements will be met:
 * http://www.gnu.org/copyleft/gpl.html.
 *
 * Other Usage
 * Alternatively, this file may be used in accordance with the terms
 * and conditions contained in a signed written agreement between
 * you and ecsec GmbH.
 *
 ***************************************************************************/

package org.openecard.richclient.gui.update;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.openecard.common.AppVersion;
import org.openecard.common.I18n;
import org.openecard.common.util.SysUtils;
import org.openecard.common.util.VersionUpdate;
import org.openecard.common.util.VersionUpdateChecker;


/**
 *
 * @author Sebastian Schuberth
 */
public class UpdateWindow {

    private final I18n lang = I18n.getTranslation("update");
    private final VersionUpdateChecker updateChecker;
    private final Stage stage;
    private double width;

    public UpdateWindow(VersionUpdateChecker checker, Stage stage) {
	this.updateChecker = checker;
	this.stage = stage;
    }

    public void init() {
	VBox sp = new VBox();
	List<Node> nodes = getElements();

	sp.getChildren().addAll(nodes);
	sp.setMaxSize(width+20, VBox.USE_PREF_SIZE);
	sp.getStyleClass().add("update");
	Scene scene = new Scene(sp, sp.getMaxWidth(), sp.getMaxHeight());
	String css = getClass().getResource("/update.css").toExternalForm();
	scene.getStylesheets().add(css);

	String icon = getClass().getResource("/images/update.jpg").toExternalForm();
	stage.getIcons().add(new Image(icon));
	stage.setTitle(lang.translationForKey("tooltip_msg"));
	stage.setScene(scene);
	stage.setResizable(false);
	stage.show();
    }

    public void toFront() {
	stage.toFront();
    }

    private List<Node> getElements() {
	final FitContentTableView<VersionUpdateTableItem> table = new FitContentTableView<>();
	TableColumn versionCol = new TableColumn(lang.translationForKey("version"));
	versionCol.setCellValueFactory(new PropertyValueFactory("version"));

	TableColumn updateTypeCol = new TableColumn(lang.translationForKey("update_type"));
	updateTypeCol.setCellValueFactory(new PropertyValueFactory("updateType"));
	TableColumn downloadLinkCol = new TableColumn(lang.translationForKey("direct_download"));
	downloadLinkCol.setCellValueFactory(new PropertyValueFactory("downloadLink"));
	downloadLinkCol.setCellFactory(new HyperlinkCell());

	table.setRowFactory((TableView<VersionUpdateTableItem> p) -> {
	    final TableRow<VersionUpdateTableItem> row = new TableRow<>();

	    row.setOnMouseClicked((MouseEvent event) -> {
		if (! row.isEmpty() && event.getButton().equals(MouseButton.PRIMARY)
			&& event.getClickCount() == 2) {
		    VersionUpdateTableItem item = row.getItem();
		    openLink(item);
		}
	    });
	    return row;
	});

	table.setOnKeyPressed((KeyEvent t) -> {
	    if (! table.getSelectionModel().isEmpty() && t.getCode() == KeyCode.ENTER) {
		VersionUpdateTableItem item = table.getSelectionModel().getSelectedItem();
		openLink(item);
	    }
	});

	table.setEditable(false);
	table.getColumns().add(versionCol);
	table.getColumns().add(updateTypeCol);
	table.getColumns().add(downloadLinkCol);

	List<VersionUpdate> updates = new ArrayList();
	VersionUpdate majUpdate = updateChecker.getMajorUpgrade();
	ObservableList<VersionUpdateTableItem> updateList = FXCollections.observableArrayList();

	if (majUpdate != null) {
	    String version = majUpdate.getVersion().toString();
	    String type = "major";
	    String link = majUpdate.getDownloadLink().toString();
	    Hyperlink hyperlink = generateHyperLink(link);

	    updateList.add(new VersionUpdateTableItem(version, type, hyperlink));

	    updates.add(majUpdate);
	}
	VersionUpdate minUpdate = updateChecker.getMinorUpgrade();

	if (minUpdate != null) {
	    String version = minUpdate.getVersion().toString();
	    String type = "minor";
	    String link = minUpdate.getDownloadLink().toString();
	    Hyperlink hyperlink = generateHyperLink(link);

	    updateList.add(new VersionUpdateTableItem(version, type, hyperlink));

	    updates.add(minUpdate);
	}
	VersionUpdate secUpdate = updateChecker.getSecurityUpgrade();

	if (secUpdate != null) {
	    String version = secUpdate.getVersion().toString();
	    String type = "security";
	    String link = secUpdate.getDownloadLink().toString();
	    Hyperlink hyperlink = generateHyperLink(link);
	    updateList.add(new VersionUpdateTableItem(version, type, hyperlink));

	    updates.add(secUpdate);
	}

	table.getColumns().forEach((column) -> column.setSortable(false));

	table.setItems(updateList);
	table.makeTableFitContent();

	width = table.getPrefWidth();

	String currentVersion;
	VersionUpdate current = updateChecker.getCurrentVersion();

	if (current != null) {
	    currentVersion = current.getVersion().getVersionString();
	} else {
	    currentVersion = AppVersion.getVersionString();
	}

	Label label = null;
	int numberOfVersions = updates.size();

	if (! updateChecker.isCurrentMaintained()) {
	    label = new Label(lang.translationForKey("version_not_maintained", currentVersion));
	} else if (numberOfVersions == 1) {
	    label = new Label(lang.translationForKey("new_version_msg", currentVersion));
	} else if (numberOfVersions > 1) {
	    label = new Label(lang.translationForKey("new_versions_msg", currentVersion));
	}
	label.wrapTextProperty().set(true);
	List<Node> result = new ArrayList<>();
	result.add(label);
	result.add(table);

	VBox vbox = new VBox();
	Label labelPage = new Label(lang.translationForKey("manual_download"));
	vbox.getChildren().add(labelPage);
	//result.add(labelPage);

	Hyperlink downloadPage = generateHyperLink(updates.get(0).getDownloadPage().toString());
	vbox.getChildren().add(downloadPage);
	result.add(vbox);

	return result;
    }

    private Hyperlink generateHyperLink(final String link) {
	EventHandler<ActionEvent> eh = (ActionEvent t) -> {
	    openLink(link);
	};

	Hyperlink hyperLink = new Hyperlink(link);
	hyperLink.setOnAction(eh);

	return hyperLink;
    }

    private void openLink(VersionUpdateTableItem item) {
	Hyperlink link = item.getDownloadLink();
	link.setVisited(true);
	String url = link.getText();
	openLink(url);
    }

    private void openLink(String url){
	SysUtils.openUrl(URI.create(url), false);
	stage.close();
    }

}
