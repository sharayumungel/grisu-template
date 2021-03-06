package grisu.frontend.view.swing.jobcreation.templates.inputPanels;

import grisu.control.ServiceInterface;
import grisu.control.exceptions.TemplateException;
import grisu.frontend.control.jobMonitoring.RunningJobManager;
import grisu.frontend.view.swing.files.GridFileSelectionDialog;
import grisu.frontend.view.swing.files.open.FileDialogManager;
import grisu.frontend.view.swing.jobcreation.templates.PanelConfig;
import grisu.frontend.view.swing.jobcreation.templates.TemplateObject;
import grisu.frontend.view.swing.jobcreation.templates.filters.Filter;
import grisu.model.GrisuRegistryManager;
import grisu.model.UserEnvironmentManager;
import grisu.model.dto.GridFile;
import grisu.model.job.JobDescription;

import java.awt.Dimension;
import java.beans.Beans;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.text.JTextComponent;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.vpac.historyRepeater.HistoryManager;

public abstract class AbstractInputPanel extends JPanel implements
PropertyChangeListener {

	static final Logger myLogger = Logger.getLogger(AbstractInputPanel.class
			.getName());

	public static final String FILE_DIALOG_LAST_DIRECTORY_KEY = "lastDirectory";

	public static final String DEFAULT_VALUE = "defaultValue";
	public static final String NAME = "name";
	public static final String TITLE = "title";
	public static final String LABEL = "label";
	public static final String PREFILLS = "prefills";
	public static final String USE_HISTORY = "useHistory"; // default: true
	public static final String FILL_WITH_DEFAULT_VALUE = "fillWithDefaultValue"; // default:
	// false
	public static final String HISTORY_ITEMS = "historyItems";
	public static final String DEPENDENCY = "dependency";
	public static final String SIZE = "size";
	public static final String IS_VISIBLE = "isVisible";
	public static final String BEAN = "property";
	public static final String IS_EDITABLE = "editable";

	public static final String APPLICATION = "application";
	public static final String TEMPLATENAME = "templatename";
	
	private static final String HELP = "help";
	public final UUID id;

	private TemplateObject template;
	private final LinkedList<Filter> filters;

	private boolean isVisible = true;
	protected final String bean;
	protected final String templateName;

	private JobDescription jobObject;

	protected final String historyManagerEntryName;

	private Map<String, String> panelProperties;

	private ServiceInterface si;
	private UserEnvironmentManager uem;
	private RunningJobManager rjm;
	private HistoryManager hm;
	private FileDialogManager fdm;

	private JButton helpLabel;
	private boolean displayHelpLabel = false;

	private boolean initFinished = false;

	private Object oldAddValue = null;

	public AbstractInputPanel(String templateName, PanelConfig config)
			throws TemplateException {

		id = UUID.randomUUID();

		this.templateName = templateName;

		if ((config == null) || (config.getFilters() == null)) {
			this.filters = new LinkedList<Filter>();
		} else {
			this.filters = config.getFilters();
		}

		if ((config == null) || (config.getProperties() == null)
				|| (config.getProperties().size() == 0)) {
			this.panelProperties = getDefaultPanelProperties();
			if (this.panelProperties == null) {
				this.panelProperties = new HashMap<String, String>();
			}
		} else {
			this.panelProperties = getDefaultPanelProperties();
			this.panelProperties.putAll(config.getProperties());
		}

		if (StringUtils.isBlank(this.panelProperties.get(NAME))) {
			this.panelProperties.put(NAME, UUID.randomUUID().toString());
			historyManagerEntryName = templateName;
		} else {
			historyManagerEntryName = templateName + "_"
					+ this.panelProperties.get(NAME);
		}

		String title = panelProperties.get(TITLE);

		if (StringUtils.isBlank(title)) {
			title = panelProperties.get(NAME);
		}

		if (!Beans.isDesignTime()) {
			// so validator displays proper name
			if (getTextComponent() != null) {
				getTextComponent().setName(title);
			} else if (getJComboBox() != null) {
				if (StringUtils.isNotBlank(title)) {
					getJComboBox().setName(title);
				}
			}
		}

		if (!StringUtils.isBlank(this.panelProperties.get(BEAN))) {
			bean = this.panelProperties.get(BEAN);
		} else {
			bean = null;
		}

		if (!StringUtils.isBlank(this.panelProperties.get(IS_VISIBLE))) {
			try {
				isVisible = Boolean.parseBoolean(this.panelProperties
						.get(IS_VISIBLE));
			} catch (final Exception e) {
				throw new TemplateException("Can't parse isVisible property: "
						+ e.getLocalizedMessage(), e);
			}
		}
		try {
			title = this.panelProperties.get(TITLE);
			setBorder(new TitledBorder(null, title, TitledBorder.LEADING,
					TitledBorder.TOP, null, null));
		} catch (final Exception e) {
			myLogger.error(e);
		}

		final String size = this.panelProperties.get(SIZE);
		if (StringUtils.isNotBlank(size)) {
			try {
				final int width = Integer.parseInt(size.substring(0,
						size.indexOf("x")).trim());
				final int height = Integer.parseInt(size.substring(
						size.indexOf("x") + 1).trim());
				setPreferredSize(new Dimension(width, height));
				setMaximumSize(new Dimension(width, height));
			} catch (final Exception e) {
				throw new TemplateException(
						"Can't parse size property for panel "
								+ this.panelProperties.get(NAME) + ": " + size);
			}
		}

		final String help = this.panelProperties.get(HELP);

		if (StringUtils.isNotBlank(help)) {

			displayHelpLabel = true;
			getHelpLabel().setToolTipText(help);
		}

	}

	protected void addHistoryValue(String value) {
		addHistoryValue(null, value);
	}

	protected void addHistoryValue(String optionalKey, String value) {

		if (StringUtils.isBlank(optionalKey)) {
			hm.addHistoryEntry(historyManagerEntryName, value);
		} else {
			hm.addHistoryEntry(historyManagerEntryName + "_" + optionalKey,
					value);
		}

	}


	protected void addValue(String bean, Object value) {

		if (!isInitFinished()) {
			return;
		}

		try {
			final Method method = jobObject.getClass().getMethod(
					"add" + StringUtils.capitalize(bean), value.getClass());
			method.invoke(jobObject, value);
			applyFilters();
		} catch (final Exception e) {
			myLogger.error(e);
		}
	}

	public void addValueToHistory() {

		final String value = getValueAsString();

		if (StringUtils.isNotBlank(value)) {
			addHistoryValue(value);
		}

	}

	private void applyFilters() {

		String string = getValueAsString();
		// System.out.println("Before filters; " + string);
		if (string == null) {
			myLogger.debug("Value is null. Not applying filters...");
			return;
		}
		for (final Filter filter : filters) {
			string = filter.filter(string);
		}

		template.userInput(getPanelName(), string);
	}

	protected boolean displayHelpLabel() {
		return displayHelpLabel;
	}

	@Override
	public boolean equals(Object o) {
		if (!( o instanceof AbstractInputPanel) ) {
			return false;
		}

		AbstractInputPanel other = (AbstractInputPanel)o;
		return id.equals(other.id);
	}

	protected boolean fillDefaultValueIntoFieldWhenPreparingPanel() {

		try {
			if (panelProperties.get(FILL_WITH_DEFAULT_VALUE) != null) {
				final boolean use = Boolean.parseBoolean(panelProperties
						.get(FILL_WITH_DEFAULT_VALUE));
				return use;
			} else {
				return false;
			}
		} catch (final Exception e) {
			return false;
		}
	}

	/**
	 * Returns a set of default values if no configuration is specified in the
	 * template.
	 *
	 * Good to have as a reference which values are available for this panel.
	 *
	 * @return the properties
	 */
	protected Map<String, String> getDefaultPanelProperties() {
		final Map<String, String> defaultProperties = new HashMap<String, String>();

		return defaultProperties;
	}

	public String getDefaultValue() {

		String last = null;

		if (useHistory()) {
			try {
				last = getLastValue();
				if (last != null) {
					return last;
				}
			} catch (final Exception e) {
				myLogger.debug("No history value for "
						+ panelProperties.get(NAME));
			}
		}

		last = panelProperties.get(DEFAULT_VALUE);
		return last;
	}

	public GridFileSelectionDialog getFileDialog() {
		return getFileDialog(templateName);
	}

	public GridFileSelectionDialog getFileDialog(String templateName) {
		
		return fdm.getFileDialog(templateName);

	}

	protected JButton getHelpLabel() {
		if (helpLabel == null) {
			helpLabel = new JButton("");
			helpLabel.setBorder(null);
			final Icon icon = new ImageIcon(
					TextField.class.getResource("/help_icon.gif"));
			helpLabel.setIcon(icon);
			helpLabel.setDisabledIcon(icon);
			helpLabel.setEnabled(false);
		}
		return helpLabel;
	}

	protected HistoryManager getHistoryManager() {
		return this.hm;
	}

	public List<String> getHistoryValues() {
		return getHistoryValues(null);
	}

	public synchronized List<String> getHistoryValues(String optionalKey) {
		if (StringUtils.isBlank(optionalKey)) {
			return hm.getEntries(historyManagerEntryName);
		} else {
			return hm.getEntries(historyManagerEntryName + "_" + optionalKey);
		}
	}

	public JComboBox getJComboBox() {
		return null;
	}

	protected JobDescription getJobSubmissionObject() {
		return jobObject;
	}

	protected String getLastValue() {
		return getLastValue(null);
	}

	protected String getLastValue(String optionalKey) {
		if (StringUtils.isBlank(optionalKey)) {
			return hm.getLastEntry(historyManagerEntryName);
		} else {
			return hm.getLastEntry(historyManagerEntryName + "_" + optionalKey);
		}
	}

	public String getPanelName() {
		return this.panelProperties.get(NAME);
	}

	protected String getPanelProperty(String key) {
		if (panelProperties == null) {
			throw new IllegalStateException(
					"Panel properties not initialized yet");
		}
		return panelProperties.get(key);
	}

	// protected Object getValue(String bean) {
	// try {
	// Method method =
	// jobObject.getClass().getMethod("get"+StringUtils.capitalize(bean));
	// return method.invoke(jobObject);
	// } catch (Exception e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// return null;
	// }
	// }

	protected RunningJobManager getRunningJobManager() {

		return this.rjm;
	}

	protected ServiceInterface getServiceInterface() {
		return this.si;
	}

	protected TemplateObject getTemplateObject() {
		return this.template;
	}

	public JTextComponent getTextComponent() {
		return null;
	}

	protected UserEnvironmentManager getUserEnvironmentManager() {
		return this.uem;
	}

	abstract protected String getValueAsString();

	public void initPanel(TemplateObject template,
			JobDescription jobObject) throws TemplateException {

		myLogger.debug("Initializing panel: " + getPanelName());

		if (si == null) {
			throw new IllegalStateException("ServiceInterface not set yet.");
		}

		this.template = template;

		// if (si != null) {
		// // needed for example for the file dialog
		// if (singletonServiceinterface == null) {
		// singletonServiceinterface = si;
		// }
		// this.si = si;
		// this.uem = GrisuRegistryManager.getDefault(si)
		// .getUserEnvironmentManager();
		// this.rjm = RunningJobManager.getDefault(si);
		// this.hm = GrisuRegistryManager.getDefault(si).getHistoryManager();

		if (useHistory()) {
			if (StringUtils.isNotBlank(panelProperties.get(HISTORY_ITEMS))) {
				try {
					final Integer max = Integer.parseInt(panelProperties
							.get(HISTORY_ITEMS));
					hm.setMaxNumberOfEntries(historyManagerEntryName, max);
				} catch (final Exception e) {
					throw new TemplateException(
							"Can't setup history management for panel "
									+ getPanelName(), e);
				}
			}
		}
		// }

		refresh(jobObject);

	}

	public boolean isDisplayed() {
		return isVisible;
	}

	public boolean isInitFinished() {
		return initFinished;
	}

	/**
	 * Must be implemented if a change in a job property would possibly change
	 * the value of one of the job properties this panel is responsible for.
	 *
	 * @param e
	 *            the property change event
	 */
	abstract protected void jobPropertyChanged(PropertyChangeEvent e);

	protected GridFile popupFileDialogAndAskForFile() {

		getFileDialog().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		getFileDialog().centerOnOwner();
		getFileDialog().setVisible(true);

		final GridFile file = getFileDialog().getSelectedFile();

		// final Set<GridFile> currentDirs =
		// getFileDialog().getCurrentDirectories();
		// if (currentDirs != null) {
		//
		// hm.addHistoryEntry(templateName + "_" +
		// FILE_DIALOG_LAST_DIRECTORY_KEY,
		// currentDir.getUrl());
		// }

		return file;
	}

	protected Set<GridFile> popupFileDialogAndAskForFiles() {

		getFileDialog().setSelectionMode(
				ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		getFileDialog().centerOnOwner();
		getFileDialog().setVisible(true);

		final Set<GridFile> files = getFileDialog().getSelectedFiles();

		// final Set<GridFile> currentDir =
		// getFileDialog().getCurrentDirectory();
		//
		// hm.addHistoryEntry(templateName + "_" +
		// FILE_DIALOG_LAST_DIRECTORY_KEY,
		// currentDir.getUrl());

		return files;
	}

	/**
	 * Implement this if the panel needs to be prepared with values from the
	 * template.
	 *
	 * @param panelProperties
	 *            the properties for the initial state of the panel
	 * @throws TemplateException
	 */
	abstract protected void preparePanel(Map<String, String> panelProperties)
			throws TemplateException;

	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		jobPropertyChanged(arg0);
	}

	public void refresh(JobDescription jobObject)
			throws TemplateException {

		myLogger.debug("Refreshing panel with new job: "
				+ getTemplateObject().getTemplateName() + "/" + getPanelName());

		initFinished = false;

		if (this.jobObject != null) {
			this.jobObject.removePropertyChangeListener(this);
		}

		this.jobObject = jobObject;

		this.jobObject.addPropertyChangeListener(this);

		myLogger.debug("Refreshing template: "
				+ getTemplateObject().getTemplateName() + "/" + getPanelName());
		templateRefresh(jobObject);

		myLogger.debug("Preparing panel: "
				+ getTemplateObject().getTemplateName() + "/" + getPanelName());
		preparePanel(panelProperties);

		initFinished = true;

		myLogger.debug("Setting initial value: "
				+ getTemplateObject().getTemplateName() + "/" + getPanelName());
		setInitialValue();
	}

	protected void removeValue(String bean, Object value) {

		if (!isInitFinished()) {
			return;
		}
		try {
			final Method method = jobObject.getClass().getMethod(
					"remove" + StringUtils.capitalize(bean), value.getClass());
			method.invoke(jobObject, value);
			applyFilters();
		} catch (final Exception e) {
			myLogger.error(e);
		}
	}

	abstract void setInitialValue() throws TemplateException;

	public void setServiceInterface(ServiceInterface si) {

		// createSingletonFileDialog(SwingUtilities.getWindowAncestor(this), si,
		// templateName);

		this.si = si;
		this.uem = GrisuRegistryManager.getDefault(si)
				.getUserEnvironmentManager();
		this.rjm = RunningJobManager.getDefault(si);
		this.hm = GrisuRegistryManager.getDefault(si).getHistoryManager();
		this.fdm = FileDialogManager.getDefault(si);
		
	}

	protected void setValue(String bean, Object value) throws TemplateException {

		if (!isInitFinished()) {
			return;
		}

		try {


			if (StringUtils.isNotBlank(bean)) {

				Method method = null;
				Class valueClass = null;
				if (value == null) {
					valueClass = String.class;
				} else {
					valueClass = value.getClass();
				}
				try {
					method = jobObject.getClass().getMethod(
							"set" + StringUtils.capitalize(bean), valueClass);
				} catch (final Exception e) {
					// try interfaces
					myLogger.debug("Trying interfaces for set method...");
					for (Class i : valueClass.getInterfaces()) {
						myLogger.debug("Interface: " + i.getSimpleName());
						try {
							method = jobObject.getClass().getMethod(
									"set" + StringUtils.capitalize(bean), i);

						} catch (Exception me) {
							myLogger.debug("No set class for bean " + bean
									+ " and interface " + i.getSimpleName());
						}
					}

					if (method == null) {
						// try add method
						method = jobObject.getClass().getMethod(
								"add" + StringUtils.capitalize(bean), valueClass);

						if (oldAddValue != null) {
							final Method removeMethod = jobObject
									.getClass()
									.getMethod(
											"remove" + StringUtils.capitalize(bean),
											oldAddValue.getClass());
							removeMethod.invoke(jobObject, oldAddValue);
						}
						oldAddValue = value;
					}
				}
				method.invoke(jobObject, value);
				
				System.out.println("SETTING: "+bean+" : "+value);
			}
			applyFilters();
		} catch (final Exception e) {
			e.printStackTrace();
			throw new TemplateException("Can't set value for property " + bean
					+ ": " + e.getLocalizedMessage(), e);
		}
	}

	abstract protected void templateRefresh(JobDescription jobObject);

	@Override
	public String toString() {

		final StringBuffer temp = new StringBuffer();

		temp.append("Name: " + getName() + "\n");
		temp.append("Class: " + this.getClass().toString() + "\n");
		temp.append("Properties: \n");
		for (final String key : panelProperties.keySet()) {
			temp.append("\t" + key + ": " + panelProperties.get(key) + "\n");
		}
		temp.append("Filters:\n");
		for (final Filter filter : filters) {
			temp.append("\tClass: " + filter.getClass().toString() + "\n");
		}

		return temp.toString();

	}

	public boolean useHistory() {

		try {
			if (panelProperties.get(USE_HISTORY) != null) {
				final boolean use = Boolean.parseBoolean(panelProperties
						.get(USE_HISTORY));
				return use;
			} else {
				return true;
			}
		} catch (final Exception e) {
			return true;
		}

	}

}
