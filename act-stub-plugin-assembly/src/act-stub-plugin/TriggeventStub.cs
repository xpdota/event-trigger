using System;
using System.ComponentModel;
using System.Diagnostics;
using System.Windows.Forms;
using Advanced_Combat_Tracker;
using System.IO;
using System.Reflection;
using RainbowMage.OverlayPlugin;

[assembly: AssemblyTitle("Triggevent Stub Launcher")]
[assembly: AssemblyDescription("A simple plugin that allows Triggevent to be launched via ACT")]
[assembly: AssemblyCompany("Who Knows")]
[assembly: AssemblyVersion("1.0.0.0")]

namespace ACT_Plugin
{
    public class TriggeventStub : UserControl, IActPluginV1
    {
        #region Designer Created Code (Avoid editing)

        /// <summary> 
        /// Required designer variable.
        /// </summary>
        private IContainer components = null;

        /// <summary> 
        /// Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }

            base.Dispose(disposing);
        }

        #region Component Designer generated code

        /// <summary> 
        /// Required method for Designer support - do not modify 
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            this._label1 = new System.Windows.Forms.Label();
            // this.textBox1 = new System.Windows.Forms.TextBox();
            _relaunchButton = new Button();
            _relaunchButton.Text = "(Re)Launch";
            _relaunchButton.Click += (sender, args) => { LaunchTe(); };
            _relaunchButton.Location = new System.Drawing.Point(3, 50);
            _relaunchButton.TabIndex = 1;

            this.SuspendLayout();
            // 
            // label1
            // 
            this._label1.AutoSize = true;
            this._label1.Location = new System.Drawing.Point(3, 0);
            this._label1.Name = "_label1";
            this._label1.Size = new System.Drawing.Size(434, 13);
            this._label1.TabIndex = 0;
            this._label1.Text =
                "This plugin acts as a stub so that you can automatically launch Triggernometry when you start ACT.";
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.Controls.Add(this._label1);
            this.Controls.Add(this._relaunchButton);
            this.Name = "PluginSample";
            this.Size = new System.Drawing.Size(686, 384);
            this.ResumeLayout(false);
            this.PerformLayout();
        }

        #endregion

        // private TextBox textBox1;

        private Label _label1;

        private Button _relaunchButton;

        // private TinyIoCContainer container;

        #endregion

        public TriggeventStub()
        {
            InitializeComponent();
        }

        Label _lblStatus; // The status label that appears in ACT's Plugin tab

        // string settingsFile = Path.Combine(ActGlobals.oFormActMain.AppDataFolder.FullName, "Config\\PluginSample.config.xml");

        // SettingsSerializer xmlSettings;

        private string _exePath;
        private Process _proc;

        private void LaunchTe()
        {
            if (IsLaunched())
            {
                _proc.CloseMainWindow();
            }

            _proc = Process.Start(_exePath);
        }

        private bool IsLaunched()
        {
            return _proc != null && !_proc.HasExited;
        }

        #region IActPluginV1 Members

        public void InitPlugin(TabPage pluginScreenSpace, Label pluginStatusText)
        {
            string location = ActGlobals.oFormActMain.PluginGetSelfData(this).pluginFile.ToString();
            string dllDir = Path.GetDirectoryName(location);
            _exePath = Path.Combine(dllDir, "triggevent.exe");
            _lblStatus = pluginStatusText; // Hand the status label's reference to our local var
            pluginScreenSpace.Controls.Add(this); // Add this UserControl to the tab ACT provides
            Dock = DockStyle.Fill; // Expand the UserControl to fill the tab's client space

            LaunchTe();

            string warning = null;

            try
            {
                TinyIoCContainer container = null;
                foreach (var entry in ActGlobals.oFormActMain.ActPlugins)
                {
                    if (entry.pluginObj != null &&
                        entry.pluginObj.GetType().FullName == "RainbowMage.OverlayPlugin.PluginLoader")
                    {
                        try
                        {
                            container = (TinyIoCContainer) entry.pluginObj.GetType().GetProperty("Container")
                                .GetValue(entry.pluginObj);
                        }
                        catch (Exception e)
                        {
                            MessageBox.Show("Unexpected error while looking for OverlayPlugin:\n" + e.Message);
                        }

                        break;
                    }
                }

                if (container == null)
                {
                    warning = "ERROR - OverlayPlugin must be installed, and must be earlier in your plugin order";
                }
                else
                {
                    IPluginConfig config = container.Resolve<IPluginConfig>();
                    if (!config.WSServerRunning)
                    {
                        warning = "ERROR - OverlayPlugin WSServer must be running";
                    }
                }
            }
            catch (Exception e)
            {
                warning = "ERROR - " + e;
            }


            _lblStatus.Text = (IsLaunched() ? "Plugin Started: " : "Not Started: ") + _exePath;
            if (warning != null)
            {
                _lblStatus.Text += "\n" + warning;
            }
        }

        public void DeInitPlugin()
        {
            _lblStatus.Text = "Plugin Exited";
            // TODO: this doesn't actually work, because launch4j starts a separate java process
            _proc.CloseMainWindow();
        }

        #endregion

    }
}