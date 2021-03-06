package grisu.frontend.view.swing.jobcreation.templates.inputPanels;

import org.apache.commons.lang3.StringUtils;

import grisu.control.exceptions.RemoteFileSystemException;
import grisu.control.exceptions.TemplateException;
import grisu.frontend.view.swing.jobcreation.templates.PanelConfig;
import grisu.model.FileManager;
import grisu.model.GrisuRegistryManager;

public class InputFileParser extends SingleInputFile {

	public InputFileParser(String name, PanelConfig config)
			throws TemplateException {
		super(name, config);
	}

	@Override
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

		if (StringUtils.isBlank(selectedFile)) {
			return;
		}

		FileManager fm = GrisuRegistryManager.getDefault(getServiceInterface())
				.getFileManager();

		String inputFileContent = null;
		try {
			inputFileContent = fm.getFileContent(selectedFile);
			String[] fileLines = inputFileContent.split("\n");
			boolean memorySpecifiedFlag=false;
			long intMem;
			for (String line : fileLines) {
				System.out.println("line: " + line);
				
				if (line.toLowerCase().contains("%mem")) {
//memory in N 8-byte words (N*8 bytes), or in KB, MB, GB, KW, MW or GW
					memorySpecifiedFlag=true;
					int startIndex = line.indexOf("=") + 1;
					String mem = line.substring(startIndex, line.length()).trim();
					
					try{
						intMem = Long.parseLong(mem);
						intMem = intMem*8;
						setValue("memory", intMem);
					}catch(NumberFormatException nfe){
						if(mem.endsWith("B")){
							setValue("memory", mem.substring(0, mem.length()-1));
						}
						else{
							try{
							intMem = Long.parseLong(mem.substring(0, mem.length()-2)); //mem-last 2 characters i.e. MW/KW/GW
							mem=mem.substring(mem.length()-2,mem.length()-1); //second last character
							intMem = intMem*8;
							mem=intMem+mem;
							setValue("memory", mem);
							}
							catch(NumberFormatException ne){
								memorySpecifiedFlag=false;
							}
						}
					}
					
				} else if (line.toLowerCase().contains("%nprocshared")) {
					int startIndex = line.indexOf("=") + 1;
					String cpus = line.substring(startIndex).trim();
					setValue("cpus", Integer.parseInt(cpus));
				}
			}
			if(!memorySpecifiedFlag){ //Default value = 256M if memory is not specified
				setValue("memory", "256M");
			}

		} catch (RemoteFileSystemException e) {
			e.printStackTrace();
		} catch (TemplateException e) {
			e.printStackTrace();
		}

		addHistoryValue(selectedFile);

	}
}
