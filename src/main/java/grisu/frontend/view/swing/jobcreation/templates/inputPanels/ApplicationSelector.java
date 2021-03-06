package grisu.frontend.view.swing.jobcreation.templates.inputPanels;

import grisu.control.exceptions.TemplateException;
import grisu.frontend.view.swing.jobcreation.templates.PanelConfig;
import grisu.jcommons.constants.Constants;
import grisu.model.GrisuRegistryManager;
import grisu.model.info.dto.Application;
import grisu.model.job.JobDescription;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.util.Arrays;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ApplicationSelector extends AbstractInputPanel {
	private JComboBox comboBox;
	private final DefaultComboBoxModel appModel = new DefaultComboBoxModel();

	private String lastExe;

	private boolean lastAppEmpty = false;
	private String[] lastAppPackages = null;

	public ApplicationSelector(String templateName, PanelConfig config)
			throws TemplateException {
		super(templateName, config);
		setLayout(new FormLayout(new ColumnSpec[] {
				FormSpecs.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("max(103dlu;default):grow"),
				FormSpecs.RELATED_GAP_COLSPEC, }, new RowSpec[] {
				FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC, }));
		add(getComboBox(), "2, 2, fill, default");

	}

	private JComboBox getComboBox() {
		if (comboBox == null) {
			comboBox = new JComboBox(appModel);
			comboBox.setPrototypeDisplayValue("xxxxxxxxxxxxxxxxxxxxx");
			comboBox.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {

					if (!isInitFinished()) {
						return;
					}

					if (ItemEvent.SELECTED == e.getStateChange()) {
						final Application app = (Application) appModel
								.getSelectedItem();
						new Thread() {
							@Override
							public void run() {

								try {
									setValue("application", app.getName());
								} catch (TemplateException e1) {
									myLogger.error(e1);
								}
							}
						}.start();

					}

				}
			});
		}
		return comboBox;
	}

	@Override
	protected String getValueAsString() {
		Application a = (Application) getComboBox().getSelectedItem();
		if (a == null) {
			return "";
		} else {
			return a.getName();
		}
	}

	@Override
	protected synchronized void jobPropertyChanged(PropertyChangeEvent e) {


		if (!isInitFinished()) {
			return;
		}

		if (Constants.EXECUTABLE_KEY.equals(e.getPropertyName())) {
			String cmdln = (String) e.getNewValue();
			if (StringUtils.isBlank(cmdln)) {
				return;
			}
			setProperApplicationPackage(cmdln);
			return;
		}
	}

	@Override
	protected void preparePanel(Map<String, String> panelProperties)
			throws TemplateException {

	}

	private synchronized void setApplicationPackage(final String appPackage) {

		if (StringUtils.isBlank(appPackage)
				|| appPackage.equals(getValueAsString())) {
			SwingUtilities.invokeLater(new Thread() {
				@Override
				public void run() {
					appModel.setSelectedItem(Application.GENERIC_APPLICATION);
				}
			});
		}

		SwingUtilities.invokeLater(new Thread() {
			@Override
			public void run() {
				if (appModel.getIndexOf(appPackage) >= 0) {
					appModel.setSelectedItem(appPackage);
				} else {
					appModel.setSelectedItem(Application.GENERIC_APPLICATION);
				}
			}
		});
	}

	private synchronized void setApplicationPackages(final String[] appPackages) {

		if ((lastAppPackages == appPackages)
				|| Arrays.equals(lastAppPackages, appPackages)) {
			return;
		}
		lastAppPackages = appPackages;

		if ((appPackages == null) || (appPackages.length == 0)) {
			if (!lastAppEmpty) {

				appModel.removeAllElements();
				appModel.addElement(Application.GENERIC_APPLICATION);
				final Application[] allApps = GrisuRegistryManager
						.getDefault(getServiceInterface())
						.getResourceInformation().getAllApplications();
				for (Application app : allApps) {
					if (appModel.getIndexOf(app) < 0) {
						appModel.addElement(app);
					}
				}
			}
			lastAppEmpty = true;

		} else {
			appModel.removeAllElements();
			for (String app : appPackages) {
				if (appModel.getIndexOf(app) < 0) {
					appModel.addElement(app);
				}
			}
			lastAppEmpty = false;
		}

	}

	@Override
	void setInitialValue() throws TemplateException {

		appModel.removeAllElements();
		appModel.addElement(Application.GENERIC_APPLICATION);
		final Application[] allApps = GrisuRegistryManager
				.getDefault(getServiceInterface()).getResourceInformation()
				.getAllApplications();
		for (Application app : allApps) {
			if (appModel.getIndexOf(app) < 0) {
				appModel.addElement(app);
			}
		}

		lastExe = null;
	}

	private void setProperApplicationPackage(final String cmdln) {
		final String exe = JobDescription.extractExecutable(cmdln);

		if ((exe != null) && exe.equals(lastExe)) {
			return;
		}
		lastExe = exe;

		new Thread() {
			@Override
			public void run() {

				myLogger.debug("TODO: set proper app package.");

				// String[] appPackages = GrisuRegistryManager
				// .getDefault(getServiceInterface())
				// .getResourceInformation()
				// .getApplicationPackageForExecutable(exe);
				//
				// // X.p("XXX" + StringUtils.join(appPackages, " - "));
				// if (appPackages.length == 0) {
				// setApplicationPackage(null);
				// return;
				// } else {
				// setApplicationPackage(appPackages[0]);
				// }

			}
		}.start();

	}

	@Override
	protected void templateRefresh(JobDescription jobObject) {

	}
}
