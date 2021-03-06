package grisu.frontend.view.swing.jobcreation.templates.inputPanels;

import grisu.control.exceptions.TemplateException;
import grisu.frontend.view.swing.jobcreation.templates.PanelConfig;
import grisu.model.job.JobDescription;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComboBox;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class Cpus extends AbstractInputPanel {
	private JComboBox comboBox;

	private boolean userInput = true;

	private Integer lastCpus = 1;

	public Cpus(String name, PanelConfig config) throws TemplateException {

		super(name, config);
		setLayout(new FormLayout(new ColumnSpec[] {
				FormSpecs.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("max(24dlu;default):grow"),
				FormSpecs.RELATED_GAP_COLSPEC, }, new RowSpec[] {
				FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC, }));
		add(getComboBox(), "2, 2, fill, fill");
	}

	private JComboBox getComboBox() {
		if (comboBox == null) {
			comboBox = new JComboBox();
			comboBox.setEditable(true);

			comboBox.getEditor().getEditorComponent()
			.addKeyListener(new KeyAdapter() {

				@Override
				public void keyReleased(KeyEvent e) {

					try {

						Object o = getComboBox().getEditor().getItem();

						String currentValue = null;
						if (o instanceof String) {
							currentValue = (String) o;
						} else {
							currentValue = ((Integer) o).toString();
						}

						if (StringUtils.isBlank(currentValue)) {
							getComboBox().getEditor().setItem("0");
							setValue("cpus", 0);
							lastCpus = 0;
							return;
						}
						Integer cpus = Integer.parseInt(currentValue);
						lastCpus = cpus;

					} catch (Exception ex) {
						getComboBox().getEditor().setItem(
								new String(lastCpus.toString()));
					}

				}
			});
			comboBox.addItemListener(new ItemListener() {

				public void itemStateChanged(ItemEvent e) {

					if (!userInput) {
						return;
					}

					if (!isInitFinished()) {
						return;
					}
					try {
						if (ItemEvent.SELECTED == e.getStateChange()) {
							final Integer value = (Integer) getComboBox()
									.getSelectedItem();
							try {
								setValue("cpus", value);
								lastCpus = value;
							} catch (final TemplateException e1) {
								myLogger.error(e1);
							}
						}
					} catch (Exception ex) {
						myLogger.debug(ex);
					}

				}
			});
		}
		return comboBox;
	}

	@Override
	protected Map<String, String> getDefaultPanelProperties() {

		final Map<String, String> defaultProperties = new HashMap<String, String>();

		defaultProperties.put(TITLE, "CPUS");
		defaultProperties.put(DEFAULT_VALUE, "1");
		defaultProperties.put(PREFILLS, "1,2,4,8,16,32");

		return defaultProperties;
	}

	@Override
	protected String getValueAsString() {

		try {
			final String result = ((Integer) (getComboBox().getSelectedItem()))
					.toString();
			return result;
		} catch (final Exception e) {
			myLogger.debug("Can't get value for panel " + getPanelName() + ": "
					+ e.getLocalizedMessage());
			return null;
		}

	}

	@Override
	protected void jobPropertyChanged(PropertyChangeEvent e) {

		userInput = false;

		if ("cpus".equals(e.getPropertyName())) {
			final int value = (Integer) e.getNewValue();
			getComboBox().setSelectedItem(value);
		}

		userInput = true;
	}

	@Override
	protected void preparePanel(Map<String, String> panelProperties) {

		getComboBox().removeAllItems();

		for (final String key : panelProperties.keySet()) {
			try {
				if (PREFILLS.equals(key)) {
					userInput = false;
					for (final String item : panelProperties.get(PREFILLS)
							.split(",")) {
						getComboBox().addItem(Integer.parseInt(item));
					}
					userInput = true;
				} else if (IS_EDITABLE.equalsIgnoreCase(key)) {
					getComboBox().setEditable(
							Boolean.parseBoolean(panelProperties.get(key)));
				}
			} catch (final Exception e) {
				myLogger.error(e);
			}
		}

	}

	@Override
	void setInitialValue() {

		final String def = getDefaultValue();
		if (StringUtils.isNotBlank(def)) {
			try {
				setValue("cpus", Integer.parseInt(def));
			} catch (final Exception e) {
				myLogger.error(e);
			}
		}

	}

	@Override
	protected void templateRefresh(JobDescription jobObject) {

		if (useHistory()) {
			addValueToHistory();
		}

	}
}
