package printClient;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.FocusManager;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

class WelcomeDialog extends JDialog implements ActionListener {

	private static final long serialVersionUID = 1L;

	private JTextField tokenField;
	private JButton tokenSubmitBtn;
	private Api api;

	public WelcomeDialog(Frame owner, Api api) {
		// Create the dialog
		super(owner, "Welcome to PrintClient", true);
		this.api = api;

		Container pane = this.getContentPane();
		pane.setLayout(new BorderLayout());
		
		JPanel innerPane = new JPanel(new BorderLayout());
		innerPane.setBorder(BorderFactory.createEmptyBorder(-10, 10, 10, 15));
		pane.add(innerPane, BorderLayout.CENTER);
		
		this.setSize(400, 170);

		// Populate the dialog
		JLabel welcomeText = new JLabel("<html><h2>Lets connect your Printer</h2><p>Fill in your printer token in the space below and we'll connect your 3D printer to the cloud.</p></html>");
		innerPane.add(welcomeText, BorderLayout.CENTER);

		JPanel southPane = new JPanel(new BorderLayout());
		innerPane.add(southPane, BorderLayout.SOUTH);

		tokenField = new TextFieldWithPrompt();
		southPane.add(tokenField, BorderLayout.CENTER);

		tokenSubmitBtn = new JButton("Cloud me up!");
		tokenSubmitBtn.addActionListener(this);
		southPane.add(tokenSubmitBtn, BorderLayout.EAST);

		// Show the dialog
		this.setLocationRelativeTo(null);
		this.setVisible(true);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		System.out.println("Setting the token: " + tokenField.getText());

		try {
			if(api.setToken(tokenField.getText()) == true)
			{
				setVisible(false);
			}
			else
			{
				JOptionPane.showMessageDialog(this.getContentPane(), "<html><h2>Yeah, so that didn't work.</h2><p>It looks like the printer token you typed in doesn't exist. Please try copying it again.</p></html>");
			}
		} catch (Exception e1) {
			e1.printStackTrace();
			JOptionPane.showMessageDialog(this.getContentPane(), "<html><h2>No Internet Connection</h2><p>We weren't able to connect to the cloud at this time. Please check that your internet connection is working and try again.</p></html>");
		}
	}

	
	// Placeholder text for jtextfield
	public class TextFieldWithPrompt extends JTextField {
		private static final long serialVersionUID = -6439222874324670386L;

		@Override
		protected void paintComponent(java.awt.Graphics g) {
			super.paintComponent(g);

			if(getText().isEmpty()) { //&& ! (FocusManager.getCurrentKeyboardFocusManager().getFocusOwner() == this)){
				Graphics2D g2 = (Graphics2D)g.create();
				g2.setBackground(Color.gray);
				g2.setFont(getFont().deriveFont(Font.ITALIC));
				g2.drawString("Sir or Madem, Your Printer Token", 7, 20); //figure out x, y from font's FontMetrics and size of component.
				g2.dispose();
			}
		}
	}
}