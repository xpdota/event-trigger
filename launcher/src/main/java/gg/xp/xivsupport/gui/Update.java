package gg.xp.xivsupport.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// This one will NOT be launched with the full classpath - it NEEDS to be self-sufficient
public class Update {
	public static void main(String[] args) throws URISyntaxException {
		JFrame frame = new JFrame("Triggevent Updater");
		frame.setSize(new Dimension(500, 200));
		frame.setLocationRelativeTo(null);
		JPanel content = new JPanel();
		content.setBorder(new EmptyBorder(10, 10, 10, 10));
		content.setLayout(new BorderLayout());
		frame.add(content);
		JLabel label = new JLabel("Foo");
		content.add(label, BorderLayout.CENTER);
		JButton button = new JButton("Wait");
		button.setPreferredSize(new Dimension(200, 40));
		JPanel buttonHolder = new JPanel();
		buttonHolder.add(button);
		content.add(buttonHolder, BorderLayout.PAGE_END);

		String path = Update.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
		label.setText(path);

		frame.setVisible(true);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		button.setEnabled(false);





	}

	private static String md5sum(String file) {
		try (FileInputStream fis = new FileInputStream("launcher-1.0-SNAPSHOT.jar")) {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			try (DigestInputStream dis = new DigestInputStream(fis, md5)) {
				while (dis.read() != -1) {
				}
			}
			byte[] md5sum = md5.digest();
			StringBuilder md5String = new StringBuilder();
			for (byte b : md5sum) {
				md5String.append(Integer.toString((b & 0xff), 16));
			}
			return md5String.toString();
		}
		catch (IOException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
}
