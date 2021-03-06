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

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class TextCombo extends AbstractInputPanel {

	private JComboBox combobox;

	private DefaultComboBoxModel model;

	private String currentValue = null;

	public TextCombo(String name, PanelConfig config) throws TemplateException {

		super(name, config);
		// setLayout(new FormLayout(new ColumnSpec[] {
		// FormSpecs.RELATED_GAP_COLSPEC,
		// ColumnSpec.decode("default:grow"),
		// FormSpecs.RELATED_GAP_COLSPEC, }, new RowSpec[] {
		// FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
		// FormSpecs.RELATED_GAP_ROWSPEC, }));

		setLayout(new FormLayout(new ColumnSpec[] {
				FormSpecs.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
				FormSpecs.RELATED_GAP_COLSPEC, }, new RowSpec[] {
				FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC, }));

		if (displayHelpLabel()) {

			add(getComboBox(), "2, 2, fill, fill");
			add(getHelpLabel(), "4, 2");
		} else {
			add(getComboBox(), "2, 2, 3, 1, fill, fill");
		}
	}

	private JComboBox getComboBox() {
		if (combobox == null) {
			if (model == null) {
				model = new DefaultComboBoxModel();
			}
			combobox = new JComboBox(model);

			combobox.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (isInitFinished()) {
						if (ItemEvent.SELECTED == e.getStateChange()) {
							try {
								currentValue = (String) model.getSelectedItem();
								setValue(bean, currentValue);
							} catch (final TemplateException e1) {
								myLogger.error(e1);
							}
						}
					}
				}
			});

			combobox.getEditor().getEditorComponent()
			.addKeyListener(new KeyAdapter() {

				@Override
				public void keyReleased(KeyEvent e) {
					try {

						// if ( StringUtils.isBlank(bean) ) {
						// return;
						// }

						currentValue = (String) getComboBox()
								.getEditor().getItem();
						setValue(bean, currentValue);
					} catch (final TemplateException e1) {
								myLogger.error(e1);
					}
				}

			});
		}
		return combobox;
	}

	@Override
	protected Map<String, String> getDefaultPanelProperties() {

		final Map<String, String> defaultProperties = new HashMap<String, String>();

		return defaultProperties;

	}

	@Override
	public JComboBox getJComboBox() {
		return getComboBox();
	}

	@Override
	protected String getValueAsString() {
		// return (String) (getComboBox().getEditor().getItem());
		return currentValue;
	}

	@Override
	protected void jobPropertyChanged(PropertyChangeEvent e) {

	}

	@Override
	protected void preparePanel(Map<String, String> panelProperties)
			throws TemplateException {

		getComboBox().removeAllItems();

		final String prefills = panelProperties.get(PREFILLS);
		if (StringUtils.isNotBlank(prefills)) {

			for (final String value : prefills.split(",")) {
				model.addElement(value);
			}

		}

		if (useHistory()) {
			for (final String value : getHistoryValues()) {
				if (model.getIndexOf(value) < 0) {
					model.addElement(value);
				}
			}
		}

		boolean isEditable = true;
		try {
			if (panelProperties.get(IS_EDITABLE) != null) {
				isEditable = Boolean.parseBoolean(panelProperties
						.get(IS_EDITABLE));
			}
		} catch (final Exception e) {
			throw new TemplateException("Can't parse \"editable\" value: "
					+ panelProperties.get(IS_EDITABLE));
		}

		if (isEditable) {
			getComboBox().setEditable(true);
		} else {
			getComboBox().setEditable(false);
		}
	}

	@Override
	void setInitialValue() throws TemplateException {

		boolean fill = false;
		try {
			if (StringUtils.isNotBlank(getPanelProperty(IS_EDITABLE))) {
				fill = !Boolean.parseBoolean(getPanelProperty(IS_EDITABLE));
			}
		} catch (final Exception e) {
			throw new TemplateException(
					"Can't parse editable value for combobox: "
							+ e.getLocalizedMessage());
		}

		if (fill || fillDefaultValueIntoFieldWhenPreparingPanel()) {

			String value = getDefaultValue();
			if (StringUtils.isBlank(value)) {
				try {
					value = (String) model.getElementAt(0);
				} catch (final Exception e) {
					myLogger.error(e);
				}
			}

			currentValue = value;
			getComboBox().setSelectedItem(value);
			setValue(bean, value);

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
