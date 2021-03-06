package grisu.frontend.view.swing.jobcreation.templates.inputPanels;

import grisu.control.JobnameHelpers;
import grisu.control.exceptions.TemplateException;
import grisu.frontend.view.swing.jobcreation.templates.PanelConfig;
import grisu.model.job.JobDescription;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComboBox;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class SimpleCommandline extends AbstractInputPanel {
	private JComboBox comboBox;

	private String lastCalculatedExecutable = null;

	public SimpleCommandline(String name, PanelConfig config)
			throws TemplateException {

		super(name, config);
		setLayout(new FormLayout(new ColumnSpec[] {
				FormSpecs.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormSpecs.RELATED_GAP_COLSPEC, }, new RowSpec[] {
				FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC, }));
		add(getComboBox(), "2, 2, fill, fill");
		// setLayout(new BorderLayout());
		// add(getComboBox(), BorderLayout.CENTER);
	}

	private void commandlineChanged() throws TemplateException {

		String commandline;
		try {
			commandline = ((String) getComboBox().getEditor().getItem()).trim();
		} catch (final Exception e) {
			myLogger.debug(e.getLocalizedMessage());
			return;
		}
		// System.out.println("Commandline changed: " + commandline);

		String exe;
		if (commandline == null) {
			exe = "";
		} else {
			final int firstWhitespace = commandline.indexOf(" ");
			if (firstWhitespace == -1) {
				exe = commandline;
			} else {
				exe = commandline.substring(0, firstWhitespace);
			}
		}

		// setting jobname if configured in widget config
		String jobnameCreate = getPanelProperty(SingleInputFile.SET_JOBNAME);
		if (StringUtils.isNotBlank(commandline)) {
			if ("true".equalsIgnoreCase(jobnameCreate)
					|| "exe".equalsIgnoreCase(jobnameCreate)) {

				String jobname = exe + "_job";
				final String sugJobname = getUserEnvironmentManager()
						.calculateUniqueJobname(jobname);

				try {
					setValue("jobname", sugJobname);
				} catch (TemplateException e) {
					myLogger.debug("Can't set jobname:"
							+ e.getLocalizedMessage());
				}

			}
		}

		setValue("commandline", commandline);

		lastCalculatedExecutable = exe;

		if (exe.length() == 0) {
			lastCalculatedExecutable = null;
			// setValue("application", "");
			// setValue("applicationVersion", "");
			getTemplateObject().userInput(getPanelName(), "");
			return;
		}

		// jobObject.setApplication(exe);
		getTemplateObject().userInput(getPanelName(), commandline);

	}

	private JComboBox getComboBox() {
		if (comboBox == null) {
			comboBox = new JComboBox();
			comboBox.setEditable(true);
			comboBox.setPrototypeDisplayValue("xxxxx");
			comboBox.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {

					if (ItemEvent.SELECTED == e.getStateChange()) {
						try {
							commandlineChanged();
						} catch (final TemplateException e1) {
							myLogger.error(e1);
						}
					}
				}
			});

			comboBox.getEditor().getEditorComponent()
					.addKeyListener(new KeyListener() {

						@Override
						public void keyPressed(KeyEvent e) {
							// System.out.println("Key pressed.");
						}

						@Override
						public void keyReleased(KeyEvent e) {
							// System.out.println("Key released.");
							try {
								commandlineChanged();
							} catch (TemplateException e1) {
								myLogger.error(e1);
							}
						}

						@Override
						public void keyTyped(KeyEvent e) {
							// System.out.println("Key typed.");
						}
					});
		}
		return comboBox;
	}

	@Override
	protected Map<String, String> getDefaultPanelProperties() {

		final Map<String, String> defaultProperties = new HashMap<String, String>();
		defaultProperties.put(TITLE, "Commandline");
		defaultProperties.put(HISTORY_ITEMS, "8");
		return defaultProperties;
	}

	@Override
	protected String getValueAsString() {
		final String value = ((String) (getComboBox().getEditor().getItem()));
		return value;
	}

	@Override
	protected void jobPropertyChanged(PropertyChangeEvent e) {

	}

	@Override
	protected void preparePanel(Map<String, String> panelProperties) {

		getComboBox().removeAllItems();

		for (final String value : getHistoryValues()) {
			getComboBox().addItem(value);
		}

	}

	@Override
	void setInitialValue() throws TemplateException {

		if (fillDefaultValueIntoFieldWhenPreparingPanel()) {
			getComboBox().setSelectedItem(getDefaultValue());
		} else {
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
