package grisu.frontend.view.swing.jobcreation.templates.inputPanels;

import grisu.control.JobnameHelpers;
import grisu.control.exceptions.RemoteFileSystemException;
import grisu.control.exceptions.TemplateException;
import grisu.frontend.view.swing.files.GridFileSelectionDialog;
import grisu.frontend.view.swing.jobcreation.templates.PanelConfig;
import grisu.model.FileManager;
import grisu.model.dto.GridFile;
import grisu.model.job.JobDescription;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class SingleInputFile extends AbstractInputPanel {

	public static final String SET_JOBNAME = "setJobname";
	public static final String SET_AS_STDIN = "setAsStdin";

	protected JComboBox comboBox;
	protected JButton button;
	protected GridFileSelectionDialog dialog;

	protected String selectedFile = null;

	protected DefaultComboBoxModel comboBoxModel;
	protected JLabel label;

	public SingleInputFile(String name, PanelConfig config)
			throws TemplateException {

		super(name, config);

		if (!displayHelpLabel()) {
			setLayout(new FormLayout(new ColumnSpec[] {
					FormSpecs.RELATED_GAP_COLSPEC,
					ColumnSpec.decode("center:max(35dlu;default):grow"),
					FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
					FormSpecs.RELATED_GAP_COLSPEC, }, new RowSpec[] {
					FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
					FormSpecs.RELATED_GAP_ROWSPEC, }));
			add(getComboBox(), "2, 2, fill, default");
			add(getButton(), "4, 2");
		} else {
			setLayout(new FormLayout(new ColumnSpec[] {
					FormSpecs.RELATED_GAP_COLSPEC,
					ColumnSpec.decode("center:max(35dlu;default):grow"),
					FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
					FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
					FormSpecs.RELATED_GAP_COLSPEC, }, new RowSpec[] {
					FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
					FormSpecs.RELATED_GAP_ROWSPEC, }));
			add(getComboBox(), "2, 2, fill, default");
			add(getButton(), "4, 2");
			add(getHelpLabel(), "6, 2");
		}

		// config.addValidator(new FileExistsValidator());
	}

	protected void fileChanged() {

		if (!isInitFinished()) {
			return;
		}

		if (selectedFile != null) {
			removeValue("inputFileUrl", selectedFile);
		}
		selectedFile = (String) getComboBox().getSelectedItem();

		addValue("inputFileUrl", selectedFile);

		if (Boolean.parseBoolean(getPanelProperty(SET_AS_STDIN))) {
			try {
				setValue("stdin", FileManager.getFilename(selectedFile));
			} catch (TemplateException e) {
				myLogger.debug("Can't set stdin value: "
						+ e.getLocalizedMessage());
				return;
			}
		}

		// setting jobname if configured in widget config
		String jobnameCreate = getPanelProperty(SingleInputFile.SET_JOBNAME);
		if (StringUtils.isNotBlank(selectedFile)) {
			if ("true".equalsIgnoreCase(jobnameCreate)
					|| "count".equalsIgnoreCase(jobnameCreate)) {

				String jobname = FilenameUtils.getBaseName(selectedFile);
				final String sugJobname = getUserEnvironmentManager()
						.calculateUniqueJobname(jobname);

				try {
					setValue("jobname", sugJobname);
				} catch (TemplateException e) {
					myLogger.debug("Can't set jobname:"
							+ e.getLocalizedMessage());
				}
			} else if ("timestamp".equalsIgnoreCase(jobnameCreate)) {
				String jobname = FilenameUtils.getBaseName(selectedFile);
				final String sugJobname = JobnameHelpers
						.calculateTimestampedJobname(jobname,
								JobnameHelpers.short_format);

				try {
					setValue("jobname", sugJobname);
				} catch (TemplateException e) {
					myLogger.debug("Can't set jobname:"
							+ e.getLocalizedMessage());
				}
			}

			addHistoryValue(selectedFile);
		}

	}

	private JButton getButton() {
		if (button == null) {
			button = new JButton("Browse");
			button.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {

					if (getServiceInterface() == null) {
						myLogger.error("ServiceInterface not set yet.");
						return;
					}

					final GridFile file = popupFileDialogAndAskForFile();

					if (file == null) {
						return;
					}

					getComboBox().setSelectedItem(file.getUrl());

				}
			});
		}
		return button;
	}

	protected JComboBox getComboBox() {
		if (comboBox == null) {
			comboBoxModel = new DefaultComboBoxModel();
			comboBox = new JComboBox(comboBoxModel);
			comboBox.setPrototypeDisplayValue("xxxxx");
			comboBox.setEditable(true);
			comboBox.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					if (ItemEvent.SELECTED == e.getStateChange()) {

						if (isInitFinished()) {
							fileChanged();
						}

					}
				}
			});

			comboBox.getEditor().getEditorComponent()
					.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent e) {
							fileChanged();
						}
					});
		}
		return comboBox;
	}

	@Override
	protected Map<String, String> getDefaultPanelProperties() {

		final Map<String, String> defaultProperties = new HashMap<String, String>();
		defaultProperties.put(TITLE, "Input file");
		defaultProperties.put(HISTORY_ITEMS, "8");

		return defaultProperties;
	}

	// private GrisuFileDialog getFileDialog() {
	//
	// if ( si == null ) {
	// return null;
	// }
	//
	// if ( dialog == null ) {
	// dialog = new GrisuFileDialog(si);
	// dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	//
	// }
	// return dialog;
	// }

	@Override
	protected String getValueAsString() {
		return ((String) getComboBox().getSelectedItem());
	}

	@Override
	protected void jobPropertyChanged(PropertyChangeEvent e) {

	}

	@Override
	protected void preparePanel(Map<String, String> panelProperties) {

		getComboBox().removeAllItems();

		final String prefills = panelProperties.get(PREFILLS);
		if (StringUtils.isNotBlank(prefills)) {

			for (final String value : prefills.split(",")) {
				comboBoxModel.addElement(value);
			}

		}

		if (useHistory()) {
			for (final String value : getHistoryValues()) {
				if (comboBoxModel.getIndexOf(value) < 0) {
					comboBoxModel.addElement(value);
				}
			}
		}

	}

	@Override
	void setInitialValue() throws TemplateException {

		if (fillDefaultValueIntoFieldWhenPreparingPanel()) {
			getJobSubmissionObject().addInputFileUrl(getDefaultValue());
			getComboBox().setSelectedItem(getDefaultValue());
		} else {
			getComboBox().addItem("");
			getComboBox().setSelectedItem("");
		}

	}

	@Override
	protected void templateRefresh(JobDescription jobObject) {

		if (useHistory()) {
			addValueToHistory();
		}
	}

}
