/*******************************************************************************
 *
 * Skeleton class for launching a Randoop session.
 *
 * Some of the code in this class is based on code from the Eclipse JUnit plugin.
 *******************************************************************************/

//
// NOTE: The current implementation is largely a copy-paste from the JUnit
//       plugin's launch configuration delegate class that launches
//       a remote JUnit session. We should adapt it for Randoop.
//
package randoop.plugin.launching;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.SocketUtil;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import randoop.plugin.RandoopActivator;

public class RandoopLaunchConfigDelegate extends AbstractJavaLaunchConfigurationDelegate {

  @Override
    public void launch(ILaunchConfiguration configuration,
                       String mode,
                       ILaunch launch,
                       IProgressMonitor monitor) throws CoreException {

    if (monitor == null) {
      monitor = new NullProgressMonitor();
    }

    monitor.beginTask(MessageFormat.format("{0}...", new String[]{configuration.getName()}), 5); //$NON-NLS-1$
    // check for cancellation
    if (monitor.isCanceled()) {
      return;
    }

    try {
                        
      monitor.subTask("verifying_attriburtes_description");
      try {
        preLaunchCheck(configuration, launch, new SubProgressMonitor(monitor, 2));
      } catch (CoreException e) {
        if (e.getStatus().getSeverity() == IStatus.CANCEL) {
          monitor.setCanceled(true);
          return;
        }
        throw e;
      }
      // check for cancellation
      if (monitor.isCanceled()) {
        return;
      }
                        
      int fPort = evaluatePort();
      launch.setAttribute(IRandoopLaunchConfigConstants.ATTR_PORT, String.valueOf(fPort));

      String mainTypeName= verifyMainTypeName(configuration);
      IVMRunner runner= getVMRunner(configuration, mode);

      File workingDir = verifyWorkingDirectory(configuration);
      String workingDirName = null;
      if (workingDir != null) {
        workingDirName= workingDir.getAbsolutePath();
      }

      // Environment variables
      String[] envp= getEnvironment(configuration);

      ArrayList vmArguments= new ArrayList();
      ArrayList programArguments= new ArrayList();
      collectExecutionArguments(configuration, vmArguments, programArguments);

      // VM-specific attributes
      Map vmAttributesMap= getVMSpecificAttributesMap(configuration);

      // Classpath
      List<String> cpList = new ArrayList<String>(Arrays.asList(getClasspath(configuration)));

      // CANNED...
      cpList.add("C:\\Documents and Settings\\Carlos\\workspace\\randoop\\bin");
      cpList.add("C:\\Documents and Settings\\Carlos\\workspace\\randoop\\lib\\plume.jar");

      String[] classpath = cpList.toArray(new String[0]);
                        
      System.out.println(Arrays.toString(classpath));

      // Create VM config
      VMRunnerConfiguration runConfig= new VMRunnerConfiguration(mainTypeName, classpath);
      runConfig.setVMArguments((String[]) vmArguments.toArray(new String[vmArguments.size()]));
      runConfig.setProgramArguments((String[]) programArguments.toArray(new String[programArguments.size()]));
      runConfig.setEnvironment(envp);
      runConfig.setWorkingDirectory(workingDirName);
      runConfig.setVMSpecificAttributesMap(vmAttributesMap);

      // Bootpath
      runConfig.setBootClassPath(getBootpath(configuration));

      // check for cancellation
      if (monitor.isCanceled()) {
        return;
      }

      // done the verification phase
      monitor.worked(1);

      monitor.subTask("create_source_locator_description");
      // set the default source locator if required
      setDefaultSourceLocator(launch, configuration);
      monitor.worked(1);

      // Launch the configuration - 1 unit of work
      runner.run(runConfig, launch, monitor);

      // check for cancellation
      if (monitor.isCanceled()) {
        return;
      }
    } finally {
      monitor.done();
    }
  }

  /**
   * Collects all VM and program arguments. Implementors can modify and add arguments.
   *
   * @param configuration the configuration to collect the arguments for
   * @param vmArguments a {@link List} of {@link String} representing the resulting VM arguments
   * @param programArguments a {@link List} of {@link String} representing the resulting program arguments
   * @exception CoreException if unable to collect the execution arguments
   */
  protected void collectExecutionArguments(ILaunchConfiguration configuration, List/*String*/ vmArguments, List/*String*/ programArguments) throws CoreException {

    // add program & VM arguments provided by getProgramArguments and getVMArguments
    String pgmArgs= getProgramArguments(configuration);
    String vmArgs= getVMArguments(configuration);
    ExecutionArguments execArgs= new ExecutionArguments(vmArgs, pgmArgs);
    vmArguments.addAll(Arrays.asList(execArgs.getVMArgumentsArray()));
    programArguments.addAll(Arrays.asList(execArgs.getProgramArgumentsArray()));

      // CANNED...
    programArguments.add("gentests"); //$NON-NLS-1$
    programArguments.add("--maxsize=50"); //$NON-NLS-1$
    programArguments.add("--forbid-null=false"); //$NON-NLS-1$
    programArguments.add("--null-ratio=0.2"); //$NON-NLS-1$
    programArguments.add("--usethreads=false"); //$NON-NLS-1$
    programArguments.add("--noprogressdisplay=true"); //$NON-NLS-1$
    programArguments.add("--inputlimit=1000"); //$NON-NLS-1$
    programArguments.add("--output-tests=fail"); //$NON-NLS-1$
    programArguments.add("--testclass=Graph"); //$NON-NLS-1$
    programArguments.add("--testclass=NameVisitor"); //$NON-NLS-1$
  }

  private int evaluatePort() throws CoreException {
    int port= SocketUtil.findFreePort();
    if (port == -1) {
      informAndAbort("error_no_socket", null, IJavaLaunchConfigurationConstants.ERR_NO_SOCKET_AVAILABLE);
    }
    return port;
  }
        
  /**
   * Performs a check on the launch configuration's attributes. If an attribute contains an invalid value, a {@link CoreException}
   * with the error is thrown.
   *
   * @param configuration the launch configuration to verify
   * @param launch the launch to verify
   * @param monitor the progress monitor to use
   * @throws CoreException an exception is thrown when the verification fails
   */
  protected void preLaunchCheck(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor) throws CoreException {
    try {
      IJavaProject javaProject= getJavaProject(configuration);
      if ((javaProject == null) || !javaProject.exists()) {
        informAndAbort("error_invalidproject", null, IJavaLaunchConfigurationConstants.ERR_NOT_A_JAVA_PROJECT);
      }
    } finally {
      monitor.done();
    }
  }

  private void informAndAbort(String message, Throwable exception, int code) throws CoreException {
    IStatus status= new Status(IStatus.INFO, RandoopActivator.PLUGIN_ID, code, message, exception);
    if (showStatusMessage(status)) {
      // Status message successfully shown
      // -> Abort with INFO exception
      // -> Worker.run() will not write to log
      throw new CoreException(status);
    } else {
      // Status message could not be shown
      // -> Abort with original exception
      // -> Will write WARNINGs and ERRORs to log
      abort(message, exception, code);
    }
  }
        
  private boolean showStatusMessage(final IStatus status) {
    final boolean[] success= new boolean[] { false };
    getDisplay().syncExec(
                          new Runnable() {
                            public void run() {
                              Shell shell= RandoopActivator.getActiveWorkbenchShell();
                              if (shell == null)
                                shell= getDisplay().getActiveShell();
                              if (shell != null) {
                                MessageDialog.openInformation(shell, "LaunchConfigurationDelegate_dialog_title", status.getMessage());
                                success[0]= true;
                              }
                            }
                          }
                          );
    return success[0];
  }
        

  private Display getDisplay() {
    Display display;
    display= Display.getCurrent();
    if (display == null)
      display= Display.getDefault();
    return display;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#verifyMainTypeName(org.eclipse.debug.core.ILaunchConfiguration)
   */
  public String verifyMainTypeName(ILaunchConfiguration configuration) throws CoreException {
    return "randoop.main.Main"; //$NON-NLS-1$
  }
}