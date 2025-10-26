import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class DarkDashboardContactBook {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DashboardFrame().setVisible(true));
    }

    // Contact class
    static class Contact implements Serializable {
        private static final long serialVersionUID = 1L;
        String name, phone, email, address;

        public Contact(String name, String phone, String email, String address) {
            this.name = name; this.phone = phone; this.email = email; this.address = address;
        }
    }

    // Manager for contacts with serialization
    static class ContactManager {
        java.util.List<Contact> contacts = new java.util.ArrayList<>();
        File file = new File("contacts.dat");

        public ContactManager() { load(); }

        @SuppressWarnings("unchecked")
        void load() {
            if(!file.exists()) return;
            try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                contacts = (java.util.List<Contact>) ois.readObject();
            } catch(Exception e) { System.err.println("Load error: " + e.getMessage()); }
        }

        void save() {
            try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                oos.writeObject(contacts);
            } catch(IOException e) { e.printStackTrace(); }
        }

        void add(Contact c) { contacts.add(c); }
        void remove(int idx) { contacts.remove(idx); }
        void update(int idx, Contact c) { contacts.set(idx, c); }
    }

    // Table model
    static class ContactTableModel extends AbstractTableModel {
        String[] cols = {"Name","Phone","Email","Address"};
        ContactManager manager;
        public ContactTableModel(ContactManager m) { manager = m; }
        public int getRowCount() { return manager.contacts.size(); }
        public int getColumnCount() { return cols.length; }
        public String getColumnName(int c){ return cols[c]; }
        public Object getValueAt(int r,int c){
            Contact ct = manager.contacts.get(r);
            switch(c){
                case 0: return ct.name;
                case 1: return ct.phone;
                case 2: return ct.email;
                case 3: return ct.address;
            } return null;
        }
        public Contact getContactAt(int r){ return manager.contacts.get(r); }
        public void refresh(){ fireTableDataChanged(); }
    }

    // Main dashboard frame
    static class DashboardFrame extends JFrame{
        ContactManager manager = new ContactManager();
        ContactTableModel model = new ContactTableModel(manager);
        JTable table = new JTable(model);
        TableRowSorter<ContactTableModel> sorter = new TableRowSorter<>(model);
        JTextField searchField = new JTextField(20);
        JTextArea detailsArea = new JTextArea(5, 50);

        public DashboardFrame(){
            super("Dark Dashboard Contact Book");
            setSize(900,600); setLocationRelativeTo(null);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            getContentPane().setBackground(new Color(34,34,34));
            setLayout(new BorderLayout());

            // Table
            table.setRowSorter(sorter);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setBackground(new Color(45,45,45));
            table.setForeground(Color.WHITE);
            table.setSelectionBackground(new Color(70,130,180));
            table.setGridColor(new Color(70,70,70));
            JScrollPane tableScroll = new JScrollPane(table);

            // Top bar buttons
            JPanel topBar = new JPanel();
            topBar.setBackground(new Color(34,34,34));
            topBar.setLayout(new FlowLayout(FlowLayout.CENTER,10,10));

            JButton addBtn = createButton("Add");
            JButton editBtn = createButton("Edit");
            JButton delBtn = createButton("Delete");

            topBar.add(new JLabel("Search:"){ { setForeground(Color.WHITE); } });
            topBar.add(searchField);
            topBar.add(addBtn);
            topBar.add(editBtn);
            topBar.add(delBtn);

            // Details panel
            detailsArea.setEditable(false);
            detailsArea.setBackground(new Color(50,50,50));
            detailsArea.setForeground(Color.WHITE);
            detailsArea.setFont(new Font("Monospaced",Font.PLAIN,13));
            detailsArea.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.WHITE),
                    "Contact Details", 0,0, new Font("SansSerif",Font.BOLD,12), Color.WHITE));

            JScrollPane detailScroll = new JScrollPane(detailsArea);

            // Dashboard layout
            JPanel centerPanel = new JPanel(new BorderLayout());
            centerPanel.setBackground(new Color(34,34,34));
            centerPanel.add(tableScroll, BorderLayout.CENTER);
            centerPanel.add(detailScroll, BorderLayout.SOUTH);

            add(topBar, BorderLayout.NORTH);
            add(centerPanel, BorderLayout.CENTER);

            // Event listeners
            addBtn.addActionListener(e -> onAdd());
            editBtn.addActionListener(e -> onEdit());
            delBtn.addActionListener(e -> onDelete());

            searchField.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e){ onSearch(); }
                public void removeUpdate(DocumentEvent e){ onSearch(); }
                public void changedUpdate(DocumentEvent e){ onSearch(); }
            });

            table.getSelectionModel().addListSelectionListener(e -> showDetails());
        }

        JButton createButton(String text){
            JButton btn = new JButton(text);
            btn.setFocusPainted(false);
            btn.setBackground(new Color(70,70,70));
            btn.setForeground(Color.WHITE);
            btn.setBorder(BorderFactory.createLineBorder(Color.GRAY,1,true));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.addMouseListener(new MouseAdapter(){
                public void mouseEntered(MouseEvent e){ btn.setBackground(new Color(0,128,128)); }
                public void mouseExited(MouseEvent e){ btn.setBackground(new Color(70,70,70)); }
            });
            return btn;
        }

        void onSearch(){
            String text = searchField.getText();
            if(text.trim().isEmpty()) sorter.setRowFilter(null);
            else sorter.setRowFilter(RowFilter.regexFilter("(?i)"+text,0,1,2,3));
        }

        void showDetails(){
            int sel = table.getSelectedRow();
            if(sel==-1){ detailsArea.setText(""); return; }
            int idx = table.convertRowIndexToModel(sel);
            Contact c = model.getContactAt(idx);
            detailsArea.setText(String.format("Name: %s\nPhone: %s\nEmail: %s\nAddress:\n%s",
                    c.name,c.phone,c.email,c.address));
        }

        void onAdd(){
            ContactForm form = new ContactForm(this,null);
            Contact c = form.showDialog();
            if(c!=null){ manager.add(c); model.refresh(); manager.save(); }
        }

        void onEdit(){
            int sel = table.getSelectedRow();
            if(sel==-1){ JOptionPane.showMessageDialog(this,"Select a contact to edit."); return; }
            int idx = table.convertRowIndexToModel(sel);
            Contact existing = model.getContactAt(idx);
            ContactForm form = new ContactForm(this,existing);
            Contact updated = form.showDialog();
            if(updated!=null){ manager.update(idx,updated); model.refresh(); manager.save(); }
        }

        void onDelete(){
            int sel = table.getSelectedRow();
            if(sel==-1){ JOptionPane.showMessageDialog(this,"Select a contact to delete."); return; }
            int idx = table.convertRowIndexToModel(sel);
            int conf = JOptionPane.showConfirmDialog(this,"Delete selected contact?","Confirm",JOptionPane.YES_NO_OPTION);
            if(conf==JOptionPane.YES_OPTION){ manager.remove(idx); model.refresh(); manager.save(); }
        }
    }

    // Contact add/edit form
    static class ContactForm {
        JDialog dialog;
        JTextField nameF = new JTextField(30), phoneF = new JTextField(20), emailF = new JTextField(30);
        JTextArea addrF = new JTextArea(4,30);
        Contact result = null;

        public ContactForm(Frame owner, Contact existing){
            dialog = new JDialog(owner,"Add/Edit Contact",true);
            JPanel p = new JPanel(new GridBagLayout());
            p.setBackground(new Color(34,34,34));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4,4,4,4); gbc.anchor = GridBagConstraints.WEST;

            gbc.gridx=0; gbc.gridy=0; p.add(new JLabel("Name:"){ { setForeground(Color.WHITE); } }, gbc);
            gbc.gridx=1; p.add(nameF,gbc);

            gbc.gridx=0; gbc.gridy=1; p.add(new JLabel("Phone:"){ { setForeground(Color.WHITE); } }, gbc);
            gbc.gridx=1; p.add(phoneF,gbc);

            gbc.gridx=0; gbc.gridy=2; p.add(new JLabel("Email:"){ { setForeground(Color.WHITE); } }, gbc);
            gbc.gridx=1; p.add(emailF,gbc);

            gbc.gridx=0; gbc.gridy=3; gbc.anchor=GridBagConstraints.NORTHWEST;
            p.add(new JLabel("Address:"){ { setForeground(Color.WHITE); } },gbc);
            gbc.gridx=1; p.add(new JScrollPane(addrF),gbc);

            JPanel btns = new JPanel(); btns.setBackground(new Color(34,34,34));
            JButton ok = new JButton("OK"); JButton cancel = new JButton("Cancel");
            btns.add(ok); btns.add(cancel);
            gbc.gridx=0; gbc.gridy=4; gbc.gridwidth=2; gbc.anchor=GridBagConstraints.CENTER;
            p.add(btns,gbc);

            if(existing!=null){ nameF.setText(existing.name); phoneF.setText(existing.phone);
                emailF.setText(existing.email); addrF.setText(existing.address); }

            ok.addActionListener(e -> onOk());
            cancel.addActionListener(e -> { result=null; dialog.dispose(); });

            dialog.getContentPane().add(p);
            dialog.pack(); dialog.setLocationRelativeTo(owner);
        }

        void onOk(){
            String n=nameF.getText().trim(), ph=phoneF.getText().trim(), em=emailF.getText().trim(), ad=addrF.getText().trim();
            if(n.isEmpty()){ JOptionPane.showMessageDialog(dialog,"Name is required"); return; }
            result = new Contact(n,ph,em,ad); dialog.dispose();
        }

        Contact showDialog(){ dialog.setVisible(true); return result; }
    }
}
