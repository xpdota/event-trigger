using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Drawing;
using System.Data;
using System.Diagnostics;
using System.Text;
using System.Windows.Forms;
using Advanced_Combat_Tracker;
using System.IO;
using System.Reflection;
using System.Xml;

[assembly: AssemblyTitle("Triggevent Stub Launcher")]
[assembly: AssemblyDescription("A simple plugin that allows Triggevent to be launched via ACT")]
[assembly: AssemblyCompany("Wynn Dohz")]
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
            this.label1 = new System.Windows.Forms.Label();
            // this.textBox1 = new System.Windows.Forms.TextBox();
            relaunchButton = new Button();
            relaunchButton.Text = "(Re)Launch";
            relaunchButton.Click += (sender, args) => { launchTE(); };
            relaunchButton.Location = new System.Drawing.Point(3, 50);
            relaunchButton.TabIndex = 1;

            this.SuspendLayout();
            // 
            // label1
            // 
            this.label1.AutoSize = true;
            this.label1.Location = new System.Drawing.Point(3, 0);
            this.label1.Name = "label1";
            this.label1.Size = new System.Drawing.Size(434, 13);
            this.label1.TabIndex = 0;
            this.label1.Text =
                "This plugin acts as a stub so that you can automatically launch Triggernometry when you start ACT.";
            // 
            // textBox1
            // 
            // this.textBox1.Location = new System.Drawing.Point(6, 16);
            // this.textBox1.Name = "textBox1";
            // this.textBox1.Size = new System.Drawing.Size(431, 20);
            // this.textBox1.TabIndex = 1;
            // this.textBox1.Text = "Sample TextBox that has its value stored to the settings file automatically.";
            // 
            // PluginSample
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            // this.Controls.Add(this.textBox1);
            this.Controls.Add(this.label1);
            this.Controls.Add(this.relaunchButton);
            this.Name = "PluginSample";
            this.Size = new System.Drawing.Size(686, 384);
            this.ResumeLayout(false);
            this.PerformLayout();
        }

        #endregion

        // private TextBox textBox1;

        private Label label1;

        private Button relaunchButton;

        #endregion

        public TriggeventStub()
        {
            InitializeComponent();
        }

        Label lblStatus; // The status label that appears in ACT's Plugin tab

        // string settingsFile = Path.Combine(ActGlobals.oFormActMain.AppDataFolder.FullName, "Config\\PluginSample.config.xml");

        // SettingsSerializer xmlSettings;

        private String exePath;
        private Process proc;

        private void launchTE()
        {
            if (isLaunched())
            {
                proc.CloseMainWindow();
            }

            proc = Process.Start(exePath);
        }

        private bool isLaunched()
        {
            return proc != null && !proc.HasExited;
        }

        #region IActPluginV1 Members

        public void InitPlugin(TabPage pluginScreenSpace, Label pluginStatusText)
        {
            // String location = typeof(TriggeventStub).Assembly.CodeBase;
            // String location = Assembly.GetExecutingAssembly().Location;
            String location = ActGlobals.oFormActMain.PluginGetSelfData(this).pluginFile.ToString();
            string dllDir = Path.GetDirectoryName(location);
            exePath = Path.Combine(dllDir, "triggevent.exe");
            lblStatus = pluginStatusText; // Hand the status label's reference to our local var
            pluginScreenSpace.Controls.Add(this); // Add this UserControl to the tab ACT provides
            Dock = DockStyle.Fill; // Expand the UserControl to fill the tab's client space
            // xmlSettings = new SettingsSerializer(this); // Create a new settings serializer and pass it this instance
            // LoadSettings();

            launchTE();

            lblStatus.Text = (isLaunched() ? "Plugin Started: " : "Not Started: ") + exePath;
        }

        public void DeInitPlugin()
        {
            // Unsubscribe from any events you listen to when exiting!
            // ActGlobals.oFormActMain.AfterCombatAction -= oFormActMain_AfterCombatAction;

            // SaveSettings();
            lblStatus.Text = "Plugin Exited";
            proc.CloseMainWindow();
        }

        #endregion

        // void oFormActMain_AfterCombatAction(bool isImport, CombatActionEventArgs actionInfo)
        // {
        //     throw new NotImplementedException();
        // }
        //
        // void LoadSettings()
        // {
        //     // Add any controls you want to save the state of
        //     xmlSettings.AddControlSetting(textBox1.Name, textBox1);
        //
        //     if (File.Exists(settingsFile))
        //     {
        //         FileStream fs = new FileStream(settingsFile, FileMode.Open, FileAccess.Read, FileShare.ReadWrite);
        //         XmlTextReader xReader = new XmlTextReader(fs);
        //
        //         try
        //         {
        //             while (xReader.Read())
        //             {
        //                 if (xReader.NodeType == XmlNodeType.Element)
        //                 {
        //                     if (xReader.LocalName == "SettingsSerializer")
        //                     {
        //                         xmlSettings.ImportFromXml(xReader);
        //                     }
        //                 }
        //             }
        //         }
        //         catch (Exception ex)
        //         {
        //             lblStatus.Text = "Error loading settings: " + ex.Message;
        //         }
        //
        //         xReader.Close();
        //     }
        // }
        //
        // void SaveSettings()
        // {
        //     FileStream fs = new FileStream(settingsFile, FileMode.Create, FileAccess.Write, FileShare.ReadWrite);
        //     XmlTextWriter xWriter = new XmlTextWriter(fs, Encoding.UTF8);
        //     xWriter.Formatting = Formatting.Indented;
        //     xWriter.Indentation = 1;
        //     xWriter.IndentChar = '\t';
        //     xWriter.WriteStartDocument(true);
        //     xWriter.WriteStartElement("Config"); // <Config>
        //     xWriter.WriteStartElement("SettingsSerializer"); // <Config><SettingsSerializer>
        //     xmlSettings.ExportToXml(xWriter); // Fill the SettingsSerializer XML
        //     xWriter.WriteEndElement(); // </SettingsSerializer>
        //     xWriter.WriteEndElement(); // </Config>
        //     xWriter.WriteEndDocument(); // Tie up loose ends (shouldn't be any)
        //     xWriter.Flush(); // Flush the file buffer to disk
        //     xWriter.Close();
        // }
    }
}