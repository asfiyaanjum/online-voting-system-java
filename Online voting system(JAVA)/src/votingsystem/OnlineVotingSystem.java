package votingsystem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;

public class OnlineVotingSystem {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DataStore store = new DataStore(Path.of("voting-data.ser"));
            VotingService service = new VotingService(store.load());
            new VotingFrame(service, store).setVisible(true);
        });
    }
}

final class VotingFrame extends JFrame {
    private final VotingService service;
    private final DataStore store;
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final JPanel adminHolder = new JPanel(new BorderLayout());
    private final JPanel voterHolder = new JPanel(new BorderLayout());
    private User currentUser;

    VotingFrame(VotingService service, DataStore store) {
        this.service = service;
        this.store = store;
        setTitle("Java Online Voting System");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1060, 700));
        setLocationRelativeTo(null);

        root.add(loginPanel(), "login");
        root.add(adminHolder, "admin");
        root.add(voterHolder, "voter");
        setContentPane(root);
        cards.show(root, "login");
    }

    private JPanel loginPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(new Color(242, 245, 248));

        JPanel panel = cardPanel(new BorderLayout(12, 12));
        panel.setPreferredSize(new Dimension(430, 370));

        JLabel title = new JLabel("Online Voting System", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        JLabel subtitle = new JLabel("Pure Java Swing application", SwingConstants.CENTER);
        subtitle.setForeground(new Color(95, 105, 118));

        JTextField userId = new JTextField();
        JPasswordField password = new JPasswordField();
        JComboBox<String> role = new JComboBox<>(new String[]{"Voter", "Admin"});
        JButton login = primaryButton("Login");
        JButton register = new JButton("Register Voter");

        JPanel fields = new JPanel(new GridLayout(0, 1, 8, 8));
        fields.setOpaque(false);
        fields.add(label("User ID"));
        fields.add(userId);
        fields.add(label("Password"));
        fields.add(password);
        fields.add(label("Role"));
        fields.add(role);

        JPanel actions = new JPanel(new GridLayout(1, 2, 8, 8));
        actions.setOpaque(false);
        actions.add(register);
        actions.add(login);

        login.addActionListener(e -> {
            try {
                currentUser = service.login(userId.getText(), new String(password.getPassword()), role.getSelectedItem().toString());
                if (currentUser.role == Role.ADMIN) {
                    refreshAdminPanel();
                    cards.show(root, "admin");
                } else {
                    refreshVoterPanel();
                    cards.show(root, "voter");
                }
                userId.setText("");
                password.setText("");
            } catch (IllegalArgumentException ex) {
                error(ex.getMessage());
            }
        });

        register.addActionListener(e -> showRegisterDialog());

        JPanel header = new JPanel(new GridLayout(0, 1, 0, 4));
        header.setOpaque(false);
        header.add(title);
        header.add(subtitle);

        panel.add(header, BorderLayout.NORTH);
        panel.add(fields, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        outer.add(panel);
        return outer;
    }

    private JPanel adminPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(topBar("Admin Dashboard"), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Election", electionTab());
        tabs.add("Candidates", candidatesTab());
        tabs.add("Voters", votersTab());
        tabs.add("Results", resultsTab());
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    private JPanel voterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(topBar("Voter Portal"), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Vote", votingTab());
        tabs.add("Results", resultsTab());
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    private JPanel topBar(String title) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(new EmptyBorder(14, 18, 14, 18));
        bar.setBackground(new Color(28, 40, 54));
        JLabel label = new JLabel(title);
        label.setForeground(Color.WHITE);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 22f));
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> {
            currentUser = null;
            cards.show(root, "login");
        });
        bar.add(label, BorderLayout.WEST);
        bar.add(logout, BorderLayout.EAST);
        return bar;
    }

    private JPanel electionTab() {
        JPanel panel = padded(new BorderLayout(12, 12));
        JTextField title = new JTextField(service.data().electionTitle);
        JTextArea notice = new JTextArea(service.data().electionNotice, 4, 40);
        JTextField start = new JTextField(service.formatDateTime(service.data().votingStart));
        JTextField end = new JTextField(service.formatDateTime(service.data().votingEnd));
        JCheckBox open = new JCheckBox("Voting is open", service.data().votingOpen);
        JButton save = primaryButton("Save Election Settings");

        JPanel form = cardPanel(new GridLayout(0, 1, 8, 8));
        form.add(label("Election Title"));
        form.add(title);
        form.add(label("Notice / Instructions"));
        form.add(new JScrollPane(notice));
        form.add(label("Voting Start Time (yyyy-MM-dd HH:mm)"));
        form.add(start);
        form.add(label("Voting End Time (yyyy-MM-dd HH:mm)"));
        form.add(end);
        form.add(open);
        form.add(save);

        save.addActionListener(e -> {
            try {
                service.updateElection(title.getText(), notice.getText(), start.getText(), end.getText(), open.isSelected());
                persist();
                info("Election settings and voting timeline saved.");
            } catch (IllegalArgumentException ex) {
                error(ex.getMessage());
            }
        });

        JPanel help = cardPanel(new BorderLayout());
        JLabel status = new JLabel(service.timelineStatus());
        status.setFont(status.getFont().deriveFont(Font.BOLD, 16f));
        help.add(status, BorderLayout.NORTH);
        help.add(new JLabel("Example time: 2026-05-08 19:30"), BorderLayout.SOUTH);

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(null);
        panel.add(help, BorderLayout.NORTH);
        panel.add(formScroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel candidatesTab() {
        JPanel panel = padded(new BorderLayout(12, 12));
        DefaultTableModel model = candidateModel();
        JTable table = new JTable(model);

        JTextField name = new JTextField();
        JTextField party = new JTextField();
        JButton add = primaryButton("Add Candidate");
        JButton remove = new JButton("Remove Selected");

        JPanel form = cardPanel(new GridLayout(0, 1, 8, 8));
        form.add(label("Candidate Name"));
        form.add(name);
        form.add(label("Party / Group"));
        form.add(party);
        form.add(add);
        form.add(remove);

        add.addActionListener(e -> {
            try {
                service.addCandidate(name.getText(), party.getText());
                persist();
                model.setDataVector(candidateRows(), new String[]{"ID", "Name", "Party", "Votes"});
                name.setText("");
                party.setText("");
            } catch (IllegalArgumentException ex) {
                error(ex.getMessage());
            }
        });
        remove.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                error("Select a candidate first.");
                return;
            }
            String id = table.getValueAt(row, 0).toString();
            try {
                service.removeCandidate(id);
                persist();
                model.setDataVector(candidateRows(), new String[]{"ID", "Name", "Party", "Votes"});
            } catch (IllegalArgumentException ex) {
                error(ex.getMessage());
            }
        });

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(form, BorderLayout.EAST);
        return panel;
    }

    private JPanel votersTab() {
        JPanel panel = padded(new BorderLayout(12, 12));
        DefaultTableModel model = voterModel();
        JTable table = new JTable(model);
        JButton refresh = primaryButton("Refresh");
        JButton delete = new JButton("Delete Selected Voter");
        refresh.addActionListener(e -> model.setDataVector(voterRows(), new String[]{"ID", "Name", "Voted"}));
        delete.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                error("Select a voter first.");
                return;
            }
            String voterId = table.getValueAt(row, 0).toString();
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Delete voter " + voterId + "?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
            try {
                service.deleteVoter(voterId);
                persist();
                model.setDataVector(voterRows(), new String[]{"ID", "Name", "Voted"});
                info("Voter deleted successfully.");
            } catch (IllegalArgumentException ex) {
                error(ex.getMessage());
            }
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(refresh);
        actions.add(delete);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel resultsTab() {
        JPanel panel = padded(new BorderLayout(12, 12));
        DefaultTableModel model = candidateModel();
        JTable table = new JTable(model);
        JButton refresh = primaryButton("Refresh Results");
        JButton export = new JButton("Export Results Text File");
        JLabel summary = new JLabel(service.summary());
        JLabel winner = new JLabel(service.winnerText());
        winner.setFont(winner.getFont().deriveFont(Font.BOLD, 20f));
        winner.setForeground(new Color(18, 115, 95));

        refresh.addActionListener(e -> {
            model.setDataVector(candidateRows(), new String[]{"ID", "Name", "Party", "Votes"});
            summary.setText(service.summary());
            winner.setText(service.winnerText());
        });
        export.addActionListener(e -> {
            try {
                Path out = service.exportResults();
                info("Results exported to " + out.toAbsolutePath());
            } catch (IOException ex) {
                error("Export failed: " + ex.getMessage());
            }
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(refresh);
        actions.add(export);
        actions.add(summary);

        JPanel top = cardPanel(new BorderLayout(8, 8));
        top.add(winner, BorderLayout.NORTH);
        top.add(actions, BorderLayout.SOUTH);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel votingTab() {
        JPanel panel = padded(new BorderLayout(12, 12));
        JLabel title = new JLabel();
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        JTextArea notice = new JTextArea();
        notice.setEditable(false);
        notice.setLineWrap(true);
        notice.setWrapStyleWord(true);
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        JButton refresh = primaryButton("Refresh");
        refresh.addActionListener(e -> fillVotingContent(title, notice, list));

        JPanel header = cardPanel(new BorderLayout(8, 8));
        header.add(title, BorderLayout.NORTH);
        header.add(new JScrollPane(notice), BorderLayout.CENTER);
        header.add(refresh, BorderLayout.EAST);

        panel.add(header, BorderLayout.NORTH);
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        SwingUtilities.invokeLater(() -> fillVotingContent(title, notice, list));
        return panel;
    }

    private void fillVotingContent(JLabel title, JTextArea notice, JPanel list) {
        title.setText(service.data().electionTitle);
        if (currentUser == null) {
            notice.setText("Please login to vote.");
            return;
        }
        notice.setText(service.data().electionNotice + "\n\n" + service.voterStatus(currentUser.id));
        list.removeAll();
        ButtonGroup group = new ButtonGroup();
        Map<JRadioButton, Candidate> buttons = new LinkedHashMap<>();
        for (Candidate candidate : service.data().candidates.values()) {
            JRadioButton option = new JRadioButton(candidate.name + " - " + candidate.party);
            option.setFont(option.getFont().deriveFont(16f));
            option.setBorder(new EmptyBorder(10, 10, 10, 10));
            group.add(option);
            buttons.put(option, candidate);
            list.add(option);
        }

        JButton vote = primaryButton("Submit Vote");
        vote.setAlignmentX(Component.LEFT_ALIGNMENT);
        vote.setEnabled(service.canVote(currentUser.id));
        vote.addActionListener(e -> {
            Candidate selected = buttons.entrySet().stream()
                    .filter(entry -> entry.getKey().isSelected())
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);
            if (selected == null) {
                error("Select a candidate before submitting.");
                return;
            }
            try {
                service.castVote(currentUser.id, selected.id);
                persist();
                info("Your vote was submitted successfully.");
                fillVotingContent(title, notice, list);
            } catch (IllegalArgumentException ex) {
                error(ex.getMessage());
            }
        });
        list.add(Box.createVerticalStrut(14));
        list.add(vote);
        list.revalidate();
        list.repaint();
    }

    private void refreshAdminPanel() {
        adminHolder.removeAll();
        adminHolder.add(adminPanel(), BorderLayout.CENTER);
        adminHolder.revalidate();
        adminHolder.repaint();
    }

    private void refreshVoterPanel() {
        voterHolder.removeAll();
        voterHolder.add(voterPanel(), BorderLayout.CENTER);
        voterHolder.revalidate();
        voterHolder.repaint();
    }

    private void showRegisterDialog() {
        JTextField id = new JTextField();
        JTextField name = new JTextField();
        JPasswordField password = new JPasswordField();
        JPanel form = new JPanel(new GridLayout(0, 1, 8, 8));
        form.add(label("Voter ID"));
        form.add(id);
        form.add(label("Full Name"));
        form.add(name);
        form.add(label("Password"));
        form.add(password);
        int result = JOptionPane.showConfirmDialog(this, form, "Register Voter", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                service.registerVoter(id.getText(), name.getText(), new String(password.getPassword()));
                persist();
                info("Voter registered. You can login now.");
            } catch (IllegalArgumentException ex) {
                error(ex.getMessage());
            }
        }
    }

    private void persist() {
        try {
            store.save(service.data());
        } catch (IOException ex) {
            error("Could not save data: " + ex.getMessage());
        }
    }

    private DefaultTableModel candidateModel() {
        return new DefaultTableModel(candidateRows(), new String[]{"ID", "Name", "Party", "Votes"}) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private Object[][] candidateRows() {
        return service.data().candidates.values().stream()
                .map(c -> new Object[]{c.id, c.name, c.party, c.votes})
                .toArray(Object[][]::new);
    }

    private DefaultTableModel voterModel() {
        return new DefaultTableModel(voterRows(), new String[]{"ID", "Name", "Voted"}) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private Object[][] voterRows() {
        return service.data().users.values().stream()
                .filter(u -> u.role == Role.VOTER)
                .map(u -> new Object[]{u.id, u.name, u.hasVoted ? "Yes" : "No"})
                .toArray(Object[][]::new);
    }

    private JPanel padded(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        panel.setBackground(new Color(242, 245, 248));
        return panel;
    }

    private JPanel cardPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBorder(new EmptyBorder(18, 18, 18, 18));
        panel.setBackground(Color.WHITE);
        return panel;
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        return label;
    }

    private JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(18, 115, 95));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        return button;
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void info(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }
}

final class VotingService {
    static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final VotingData data;

    VotingService(VotingData data) {
        this.data = data;
    }

    VotingData data() {
        return data;
    }

    User login(String id, String password, String roleName) {
        User user = data.users.get(clean(id));
        if (user == null || !user.password.equals(password)) {
            throw new IllegalArgumentException("Invalid user ID or password.");
        }
        Role requested = roleName.equalsIgnoreCase("Admin") ? Role.ADMIN : Role.VOTER;
        if (user.role != requested) {
            throw new IllegalArgumentException("Selected role does not match this account.");
        }
        return user;
    }

    void registerVoter(String id, String name, String password) {
        id = clean(id);
        require(id, "Voter ID is required.");
        require(name, "Name is required.");
        require(password, "Password is required.");
        if (data.users.containsKey(id)) {
            throw new IllegalArgumentException("This voter ID already exists.");
        }
        data.users.put(id, new User(id, name.trim(), password, Role.VOTER));
    }

    void deleteVoter(String id) {
        id = clean(id);
        User voter = data.users.get(id);
        if (voter == null || voter.role != Role.VOTER) {
            throw new IllegalArgumentException("Voter not found.");
        }
        if (voter.hasVoted) {
            throw new IllegalArgumentException("Cannot delete this voter because they have already voted.");
        }
        data.users.remove(id);
    }

    void updateElection(String title, String notice, String start, String end, boolean open) {
        require(title, "Election title is required.");
        LocalDateTime startTime = parseDateTime(start, "Voting start time is required.");
        LocalDateTime endTime = parseDateTime(end, "Voting end time is required.");
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Voting end time must be after start time.");
        }
        data.electionTitle = title.trim();
        data.electionNotice = notice == null ? "" : notice.trim();
        data.votingStart = startTime;
        data.votingEnd = endTime;
        data.votingOpen = open;
    }

    void addCandidate(String name, String party) {
        require(name, "Candidate name is required.");
        require(party, "Party or group is required.");
        String id = "C" + (data.nextCandidateNumber++);
        data.candidates.put(id, new Candidate(id, name.trim(), party.trim()));
    }

    void removeCandidate(String id) {
        Candidate candidate = data.candidates.get(id);
        if (candidate == null) {
            throw new IllegalArgumentException("Candidate not found.");
        }
        if (candidate.votes > 0) {
            throw new IllegalArgumentException("Cannot remove a candidate after votes have been cast.");
        }
        data.candidates.remove(id);
    }

    void castVote(String voterId, String candidateId) {
        if (!isVotingAllowedNow()) {
            throw new IllegalArgumentException(timelineStatus());
        }
        User voter = data.users.get(voterId);
        if (voter == null || voter.role != Role.VOTER) {
            throw new IllegalArgumentException("Only registered voters can vote.");
        }
        if (voter.hasVoted) {
            throw new IllegalArgumentException("This voter has already voted.");
        }
        Candidate candidate = data.candidates.get(candidateId);
        if (candidate == null) {
            throw new IllegalArgumentException("Candidate not found.");
        }
        candidate.votes++;
        voter.hasVoted = true;
        data.auditLog.add(LocalDateTime.now() + " | " + voter.id + " voted for " + candidate.id);
    }

    boolean canVote(String voterId) {
        User voter = data.users.get(voterId);
        return isVotingAllowedNow() && voter != null && voter.role == Role.VOTER && !voter.hasVoted && !data.candidates.isEmpty();
    }

    String voterStatus(String voterId) {
        User voter = data.users.get(voterId);
        if (!isVotingAllowedNow()) {
            return timelineStatus();
        }
        if (data.candidates.isEmpty()) {
            return "No candidates have been added yet.";
        }
        if (voter != null && voter.hasVoted) {
            return "You have already voted. Thank you. Open the Results tab to view the result.";
        }
        return "Voting is active now. Choose one candidate and submit your vote.";
    }

    String summary() {
        int voters = (int) data.users.values().stream().filter(u -> u.role == Role.VOTER).count();
        int voted = (int) data.users.values().stream().filter(u -> u.role == Role.VOTER && u.hasVoted).count();
        int totalVotes = data.candidates.values().stream().mapToInt(c -> c.votes).sum();
        return "Registered voters: " + voters + " | Voted: " + voted + " | Total votes: " + totalVotes;
    }

    String winnerText() {
        if (data.candidates.isEmpty()) {
            return "No candidates available.";
        }
        int highest = data.candidates.values().stream().mapToInt(c -> c.votes).max().orElse(0);
        if (highest == 0) {
            return "No votes have been cast yet.";
        }
        List<Candidate> leaders = data.candidates.values().stream()
                .filter(c -> c.votes == highest)
                .toList();
        if (leaders.size() > 1) {
            String names = String.join(", ", leaders.stream().map(c -> c.name).toList());
            return "Election tied between: " + names + " with " + highest + " votes each.";
        }
        Candidate winner = leaders.get(0);
        String status = LocalDateTime.now().isBefore(data.votingEnd) ? "Current leader" : "Winner";
        return status + ": " + winner.name + " (" + winner.party + ") with " + winner.votes + " votes.";
    }

    String timelineStatus() {
        LocalDateTime now = LocalDateTime.now();
        if (!data.votingOpen) {
            return "Voting is manually closed by admin.";
        }
        if (now.isBefore(data.votingStart)) {
            return "Voting has not started. Starts at " + formatDateTime(data.votingStart) + ".";
        }
        if (now.isAfter(data.votingEnd)) {
            return "Voting has ended at " + formatDateTime(data.votingEnd) + ".";
        }
        return "Voting is active until " + formatDateTime(data.votingEnd) + ".";
    }

    String formatDateTime(LocalDateTime value) {
        return value.format(DATE_TIME);
    }

    Path exportResults() throws IOException {
        Path out = Path.of("voting-results-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now()) + ".txt");
        List<String> lines = new ArrayList<>();
        lines.add(data.electionTitle);
        lines.add("Timeline: " + formatDateTime(data.votingStart) + " to " + formatDateTime(data.votingEnd));
        lines.add(summary());
        lines.add(winnerText());
        lines.add("");
        for (Candidate candidate : data.candidates.values()) {
            lines.add(candidate.id + " | " + candidate.name + " | " + candidate.party + " | Votes: " + candidate.votes);
        }
        lines.add("");
        lines.add("Audit log:");
        lines.addAll(data.auditLog);
        Files.write(out, lines);
        return out;
    }

    private boolean isVotingAllowedNow() {
        LocalDateTime now = LocalDateTime.now();
        return data.votingOpen && !now.isBefore(data.votingStart) && !now.isAfter(data.votingEnd);
    }

    private LocalDateTime parseDateTime(String value, String emptyMessage) {
        require(value, emptyMessage);
        try {
            return LocalDateTime.parse(value.trim(), DATE_TIME);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Use date/time format yyyy-MM-dd HH:mm. Example: 2026-05-08 19:30");
        }
    }

    private void require(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

final class DataStore {
    private final Path file;

    DataStore(Path file) {
        this.file = file;
    }

    VotingData load() {
        if (!Files.exists(file)) {
            return VotingData.seed();
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            VotingData data = (VotingData) in.readObject();
            data.ensureDefaults();
            return data;
        } catch (IOException | ClassNotFoundException ex) {
            JOptionPane.showMessageDialog(null, "Old saved data could not be loaded. Starting with fresh demo data.\n" + ex.getMessage());
            return VotingData.seed();
        }
    }

    void save(VotingData data) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(file))) {
            out.writeObject(data);
        }
    }
}

final class VotingData implements Serializable {
    String electionTitle = "Student Council Election";
    String electionNotice = "Login as a voter and choose exactly one candidate. Each voter can vote only once.";
    boolean votingOpen = true;
    LocalDateTime votingStart = LocalDateTime.now().minusMinutes(10);
    LocalDateTime votingEnd = LocalDateTime.now().plusHours(2);
    int nextCandidateNumber = 4;
    Map<String, User> users = new LinkedHashMap<>();
    Map<String, Candidate> candidates = new LinkedHashMap<>();
    List<String> auditLog = new ArrayList<>();

    static VotingData seed() {
        VotingData data = new VotingData();
        data.ensureDefaults();
        data.users.put("voter1", new User("voter1", "Sample Voter One", "vote123", Role.VOTER));
        data.users.put("voter2", new User("voter2", "Sample Voter Two", "vote123", Role.VOTER));
        data.candidates.put("C1", new Candidate("C1", "Asha Mehta", "Unity Group"));
        data.candidates.put("C2", new Candidate("C2", "Rahul Sharma", "Progress Panel"));
        data.candidates.put("C3", new Candidate("C3", "Neha Khan", "Independent"));
        return data;
    }

    void ensureDefaults() {
        if (users == null) {
            users = new LinkedHashMap<>();
        }
        if (candidates == null) {
            candidates = new LinkedHashMap<>();
        }
        if (auditLog == null) {
            auditLog = new ArrayList<>();
        }
        users.put("admin", new User("admin", "Election Administrator", "admin123", Role.ADMIN));
        if (electionTitle == null || electionTitle.trim().isEmpty()) {
            electionTitle = "Student Council Election";
        }
        if (electionNotice == null) {
            electionNotice = "";
        }
        if (votingStart == null) {
            votingStart = LocalDateTime.now().minusMinutes(10);
        }
        if (votingEnd == null || !votingEnd.isAfter(votingStart)) {
            votingEnd = LocalDateTime.now().plusHours(2);
        }
        if (nextCandidateNumber < 1) {
            nextCandidateNumber = candidates.size() + 1;
        }
    }
}

final class User implements Serializable {
    final String id;
    final String name;
    final String password;
    final Role role;
    boolean hasVoted;

    User(String id, String name, String password, Role role) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.role = role;
    }
}

final class Candidate implements Serializable {
    final String id;
    final String name;
    final String party;
    int votes;

    Candidate(String id, String name, String party) {
        this.id = id;
        this.name = name;
        this.party = party;
    }
}

enum Role {
    ADMIN,
    VOTER
}
