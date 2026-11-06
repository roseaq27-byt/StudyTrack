import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * StudyTrack — Personal Study Organizer
 * One-file beginner-friendly Java Swing application.
 * Data is stored in studytrack_data.dat in the working directory.
 */
 class StudyTrack extends JFrame {
    private static final String DATA_FILE = "studytrack_data.dat";
    private static final Color NAVY = new Color(32, 48, 76);
    private static final Color BLUE = new Color(66, 117, 210);
    private static final Color BG = new Color(245, 247, 251);
    private static final Color GREEN = new Color(53, 151, 115);

    // Small serializable data classes keep saved records together in this one source file.
    static class Plan implements Serializable {
        String day, subject, topic, time, status;
        int minutes;
        Plan(String d, String s, String t, String tm, int m, String st) {
            day=d; subject=s; topic=t; time=tm; minutes=m; status=st;
        }
    }
    static class Subject implements Serializable {
        String name, description;
        Subject(String n, String d) { name=n; description=d; }
    }
    static class StudySession implements Serializable {
        String subject, topic, date, start, end, notes;
        int minutes, rating;
        StudySession(String s, String t, String d, String a, String b, int m, String n, int r) {
            subject=s; topic=t; date=d; start=a; end=b; minutes=m; notes=n; rating=r;
        }
    }
    static class AppData implements Serializable {
        ArrayList<Plan> plans = new ArrayList<>();
        ArrayList<Subject> subjects = new ArrayList<>();
        ArrayList<StudySession> sessions = new ArrayList<>();
    }

    private AppData data = new AppData();
    private JTabbedPane tabs;
    private DefaultTableModel planModel, subjectModel, historyModel;
    private JTable planTable, subjectTable, historyTable;
    private JLabel dashboardSummary, dashboardWeek;
    private JPanel statsPanel;
    private JComboBox<String> planSubject, sessionSubject;
    private JTextField planTopic, planTime, planMinutes;
    private JComboBox<String> planDay, planStatus;
    private JTextField subjectName;
    private JTextArea subjectDescription;
    private JTextField sessionTopic, sessionDate, sessionStart, sessionEnd, sessionMinutes;
    private JTextArea sessionNotes;
    private JComboBox<Integer> sessionRating;
    private boolean loadingSubjects = false;

    public StudyTrack() {
        super("StudyTrack — Personal Study Organizer");
        loadData();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 700));
        setSize(1180, 760);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        buildInterface();
        refreshAll();
    }

    private void buildInterface() {
        setLayout(new BorderLayout());
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(NAVY);
        header.setBorder(new EmptyBorder(17, 22, 17, 22));
        JLabel title = new JLabel("StudyTrack");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        JLabel subtitle = new JLabel("Personal Study Organizer");
        subtitle.setForeground(new Color(205, 216, 235));
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JPanel labels = new JPanel(new GridLayout(2,1));
        labels.setOpaque(false);
        labels.add(title); labels.add(subtitle);
        header.add(labels, BorderLayout.WEST);
        JLabel date = new JLabel(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")));
        date.setForeground(Color.WHITE);
        header.add(date, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 13));
        tabs.addTab("Dashboard", dashboardPanel());
        tabs.addTab("Weekly Timetable", timetablePanel());
        tabs.addTab("My Subjects", subjectsPanel());
        tabs.addTab("Record Progress", progressPanel());
        tabs.addTab("Statistics", statisticsPanel());
        tabs.addTab("Study History", historyPanel());
        add(tabs, BorderLayout.CENTER);

        JLabel footer = new JLabel("  Your learning, organized.  •  Data saves automatically");
        footer.setBorder(new EmptyBorder(8, 10, 8, 10));
        footer.setForeground(new Color(100, 110, 128));
        footer.setFont(new Font("SansSerif", Font.PLAIN, 11));
        add(footer, BorderLayout.SOUTH);
    }

    private JPanel basePanel() {
        JPanel p = new JPanel(new BorderLayout(12,12));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(18,18,18,18));
        return p;
    }
    private JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 20));
        l.setForeground(NAVY);
        return l;
    }
    private JButton button(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setBackground(BLUE);
        b.setForeground(Color.WHITE);
        b.setBorder(new EmptyBorder(9,14,9,14));
        return b;
    }
    private DefaultTableModel tableModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
    }
    private JTable makeTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setRowHeight(27);
        t.setFont(new Font("SansSerif", Font.PLAIN, 12));
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        t.getTableHeader().setBackground(new Color(228,234,244));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setAutoCreateRowSorter(true);
        return t;
    }
    private JPanel dashboardPanel() {
        JPanel p = basePanel();
        p.add(heading("Welcome back to StudyTrack"), BorderLayout.NORTH);
        JPanel center = new JPanel(new GridLayout(2,1,12,12));
        center.setOpaque(false);
        dashboardSummary = new JLabel("", SwingConstants.CENTER);
        dashboardSummary.setFont(new Font("SansSerif", Font.BOLD, 17));
        dashboardSummary.setForeground(NAVY);
        dashboardSummary.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220,226,237)),
                new EmptyBorder(25,20,25,20)));
        dashboardSummary.setOpaque(true);
        dashboardSummary.setBackground(Color.WHITE);
        dashboardWeek = new JLabel("", SwingConstants.CENTER);
        dashboardWeek.setFont(new Font("SansSerif", Font.PLAIN, 15));
        dashboardWeek.setForeground(new Color(75,88,110));
        dashboardWeek.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220,226,237)),
                new EmptyBorder(25,20,25,20)));
        dashboardWeek.setOpaque(true);
        dashboardWeek.setBackground(Color.WHITE);
        center.add(dashboardSummary); center.add(dashboardWeek);
        p.add(center, BorderLayout.CENTER);
        return p;
    }

    private JPanel timetablePanel() {
        JPanel p = basePanel();
        p.add(heading("Weekly Timetable"), BorderLayout.NORTH);
        planModel = tableModel("Day","Subject","Topic or Course Item","Planned Study Time","Duration (min)","Status");
        planTable = makeTable(planModel);
        p.add(new JScrollPane(planTable), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createTitledBorder("Study plan details"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5,6,5,6); c.fill = GridBagConstraints.HORIZONTAL;
        planDay = new JComboBox<>(new String[]{"Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"});
        planSubject = new JComboBox<>();
        planTopic = new JTextField(14); planTime = new JTextField("09:00",8); planMinutes = new JTextField("60",6);
        planStatus = new JComboBox<>(new String[]{"Planned","In Progress","Completed"});
        addField(form,c,0,0,"Day",planDay); addField(form,c,1,0,"Subject",planSubject);
        addField(form,c,2,0,"Topic",planTopic); addField(form,c,3,0,"Time (HH:mm)",planTime);
        addField(form,c,4,0,"Duration (min)",planMinutes); addField(form,c,5,0,"Status",planStatus);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);
        JButton add=button("Add Plan"), edit=button("Edit Selected"), del=button("Delete Selected"), clear=button("Clear");
        actions.add(add); actions.add(edit); actions.add(del); actions.add(clear);
        c.gridx=0;c.gridy=2;c.gridwidth=6;c.weightx=1;form.add(actions,c);
        JPanel south = new JPanel(new BorderLayout()); south.setOpaque(false);
        south.add(form,BorderLayout.CENTER);
        p.add(south,BorderLayout.SOUTH);
        add.addActionListener(e -> addPlan());
        edit.addActionListener(e -> editPlan());
        del.addActionListener(e -> deletePlan());
        clear.addActionListener(e -> clearPlanForm());
        planTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount()==2) loadSelectedPlanToForm();
            }
        });
        return p;
    }

    private void addField(JPanel panel, GridBagConstraints c, int x, int y, String label, JComponent field) {
        c.gridx=x;c.gridy=y;c.gridwidth=1;c.weightx=0;
        panel.add(new JLabel(label),c);
        c.gridx=x;c.gridy=y+1;c.weightx=1;
        panel.add(field,c);
    }

    private JPanel subjectsPanel() {
        JPanel p=basePanel(); p.add(heading("My Subjects"),BorderLayout.NORTH);
        subjectModel=tableModel("Subject","Description");
        subjectTable=makeTable(subjectModel);
        p.add(new JScrollPane(subjectTable),BorderLayout.CENTER);
        JPanel form=new JPanel(new GridBagLayout());form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createTitledBorder("Add a subject"));
        subjectName=new JTextField(18);subjectDescription=new JTextArea(3,24);subjectDescription.setLineWrap(true);
        GridBagConstraints c=new GridBagConstraints();c.insets=new Insets(5,7,5,7);c.fill=GridBagConstraints.HORIZONTAL;
        c.gridx=0;c.gridy=0;form.add(new JLabel("Subject name"),c);
        c.gridx=1;c.weightx=1;form.add(subjectName,c);
        c.gridx=0;c.gridy=1;c.weightx=0;form.add(new JLabel("Description (optional)"),c);
        c.gridx=1;c.weightx=1;form.add(new JScrollPane(subjectDescription),c);
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));buttons.setOpaque(false);
        JButton add=button("Add Subject"),del=button("Delete Selected");
        buttons.add(add);buttons.add(del);
        c.gridx=0;c.gridy=2;c.gridwidth=2;form.add(buttons,c);
        p.add(form,BorderLayout.SOUTH);
        add.addActionListener(e->addSubject());del.addActionListener(e->deleteSubject());
        return p;
    }

    private JPanel progressPanel() {
        JPanel p=basePanel();p.add(heading("Record Study Progress"),BorderLayout.NORTH);
        JPanel form=new JPanel(new GridBagLayout());form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(12,18,12,18));
        sessionSubject=new JComboBox<>();sessionTopic=new JTextField(20);
        sessionDate=new JTextField(LocalDate.now().toString(),12);
        sessionStart=new JTextField("09:00",8);sessionEnd=new JTextField("10:00",8);
        sessionMinutes=new JTextField("",8);
        sessionNotes=new JTextArea(4,24);sessionNotes.setLineWrap(true);
        sessionRating=new JComboBox<>(new Integer[]{0,1,2,3,4,5});
        sessionRating.setToolTipText("0 means no rating");
        GridBagConstraints c=new GridBagConstraints();c.insets=new Insets(7,8,7,8);c.fill=GridBagConstraints.HORIZONTAL;
        addField(form,c,0,0,"Subject",sessionSubject);
        addField(form,c,1,0,"Topic studied",sessionTopic);
        addField(form,c,2,0,"Date (yyyy-MM-dd)",sessionDate);
        addField(form,c,0,2,"Start time (HH:mm)",sessionStart);
        addField(form,c,1,2,"End time (HH:mm)",sessionEnd);
        addField(form,c,2,2,"Duration (min, optional)",sessionMinutes);
        addField(form,c,0,4,"Self-rating (0 = none)",sessionRating);
        c.gridx=1;c.gridy=4;c.gridwidth=2;form.add(new JLabel("Notes about what you learned"),c);
        c.gridx=1;c.gridy=5;c.gridwidth=2;c.weightx=1;form.add(new JScrollPane(sessionNotes),c);
        JButton save=button("Save Study Session");
        c.gridx=0;c.gridy=6;c.gridwidth=3;c.weightx=0;form.add(save,c);
        p.add(form,BorderLayout.CENTER);
        JLabel hint=new JLabel("Duration is calculated from start/end times when possible. Enter a duration to override it.");
        hint.setForeground(new Color(95,105,120));p.add(hint,BorderLayout.SOUTH);
        save.addActionListener(e->saveSession());
        return p;
    }

    private JPanel statisticsPanel() {
        JPanel p=basePanel();p.add(heading("Study Statistics"),BorderLayout.NORTH);
        statsPanel=new JPanel(new GridLayout(2,2,12,12));statsPanel.setBackground(BG);
        p.add(statsPanel,BorderLayout.CENTER);
        return p;
    }
    private JPanel historyPanel() {
        JPanel p=basePanel();p.add(heading("Study History"),BorderLayout.NORTH);
        historyModel=tableModel("Subject","Topic","Date","Start","End","Duration (min)","Notes","Rating");
        historyTable=makeTable(historyModel);
        p.add(new JScrollPane(historyTable),BorderLayout.CENTER);
        JButton del=button("Delete Selected Session");
        JPanel bottom=new JPanel(new FlowLayout(FlowLayout.RIGHT));bottom.setOpaque(false);bottom.add(del);
        p.add(bottom,BorderLayout.SOUTH);
        del.addActionListener(e->deleteSession());
        return p;
    }

    private void addPlan() {
        try {
            String topic=planTopic.getText().trim(), time=planTime.getText().trim();
            int mins=Integer.parseInt(planMinutes.getText().trim());
            if(planSubject.getSelectedItem()==null) throw new IllegalArgumentException("Add a subject first.");
            if(topic.isEmpty()) throw new IllegalArgumentException("Enter a topic or course item.");
            LocalTime.parse(time);
            if(mins<=0) throw new IllegalArgumentException("Duration must be greater than zero.");
            data.plans.add(new Plan((String)planDay.getSelectedItem(),(String)planSubject.getSelectedItem(),topic,time,mins,(String)planStatus.getSelectedItem()));
            saveData();refreshAll();clearPlanForm();
            message("Study plan added.");
        } catch(Exception ex) { error("Please check the plan details. " + safeMessage(ex)); }
    }
    private void editPlan() {
        int row=selectedModelRow(planTable);
        if(row<0){error("Select a study plan to edit.");return;}
        try {
            String topic=planTopic.getText().trim(),time=planTime.getText().trim();
            int mins=Integer.parseInt(planMinutes.getText().trim());
            if(planSubject.getSelectedItem()==null||topic.isEmpty()||mins<=0)throw new IllegalArgumentException("Complete all required fields and use a positive duration.");
            LocalTime.parse(time);
            Plan old=data.plans.get(row);
            old.day=(String)planDay.getSelectedItem();old.subject=(String)planSubject.getSelectedItem();
            old.topic=topic;old.time=time;old.minutes=mins;old.status=(String)planStatus.getSelectedItem();
            saveData();refreshAll();message("Study plan updated.");
        }catch(Exception ex){error("Please check the plan details. "+safeMessage(ex));}
    }
    private void deletePlan() {
        int row=selectedModelRow(planTable);if(row<0){error("Select a plan to delete.");return;}
        if(confirm("Delete the selected study plan?")){data.plans.remove(row);saveData();refreshAll();}
    }
    private void loadSelectedPlanToForm() {
        int row=selectedModelRow(planTable);if(row<0)return;Plan a=data.plans.get(row);
        planDay.setSelectedItem(a.day);planSubject.setSelectedItem(a.subject);planTopic.setText(a.topic);
        planTime.setText(a.time);planMinutes.setText(String.valueOf(a.minutes));planStatus.setSelectedItem(a.status);
    }
    private void clearPlanForm(){planTopic.setText("");planTime.setText("09:00");planMinutes.setText("60");planStatus.setSelectedItem("Planned");}

    private void addSubject() {
        String name=subjectName.getText().trim();
        if(name.isEmpty()){error("Subject name cannot be empty.");return;}
        for(Subject s:data.subjects)if(s.name.equalsIgnoreCase(name)){error("That subject already exists.");return;}
        data.subjects.add(new Subject(name,subjectDescription.getText().trim()));
        subjectName.setText("");subjectDescription.setText("");saveData();refreshAll();message("Subject added.");
    }
    private void deleteSubject() {
        int row=selectedModelRow(subjectTable);if(row<0){error("Select a subject to delete.");return;}
        String name=data.subjects.get(row).name;
        if(confirm("Delete '"+name+"'? Existing plans and sessions will remain in your records.")){
            data.subjects.remove(row);saveData();refreshAll();
        }
    }

    private void saveSession() {
        try {
            if(sessionSubject.getSelectedItem()==null)throw new IllegalArgumentException("Add a subject first.");
            String topic=sessionTopic.getText().trim();if(topic.isEmpty())throw new IllegalArgumentException("Enter the topic studied.");
            LocalDate date=LocalDate.parse(sessionDate.getText().trim());
            LocalTime start=LocalTime.parse(sessionStart.getText().trim());
            LocalTime end=LocalTime.parse(sessionEnd.getText().trim());
            int mins;
            if(!sessionMinutes.getText().trim().isEmpty()) mins=Integer.parseInt(sessionMinutes.getText().trim());
            else {
                mins=(int)Duration.between(start,end).toMinutes();
                if(mins<=0)throw new IllegalArgumentException("End time must be after start time, or enter duration manually.");
            }
            if(mins<=0)throw new IllegalArgumentException("Duration must be greater than zero.");
            int rating=(Integer)sessionRating.getSelectedItem();
            data.sessions.add(new StudySession((String)sessionSubject.getSelectedItem(),topic,date.toString(),
                    start.toString(),end.toString(),mins,sessionNotes.getText().trim(),rating));
            saveData();refreshAll();
            sessionTopic.setText("");sessionNotes.setText("");sessionMinutes.setText("");
            message("Study session saved.");
        }catch(Exception ex){error("Please check the session details. "+safeMessage(ex));}
    }
    private void deleteSession() {
        int row=selectedModelRow(historyTable);if(row<0){error("Select a history entry to delete.");return;}
        if(confirm("Delete the selected study session?")){data.sessions.remove(row);saveData();refreshAll();}
    }

    private int selectedModelRow(JTable table) {
        int r=table.getSelectedRow();return r<0?-1:table.convertRowIndexToModel(r);
    }
    private void refreshAll() {
        if(planModel!=null){planModel.setRowCount(0);for(Plan a:data.plans)planModel.addRow(new Object[]{a.day,a.subject,a.topic,a.time,a.minutes,a.status});}
        if(subjectModel!=null){subjectModel.setRowCount(0);for(Subject s:data.subjects)subjectModel.addRow(new Object[]{s.name,s.description});}
        if(historyModel!=null){historyModel.setRowCount(0);for(StudySession s:data.sessions)historyModel.addRow(new Object[]{s.subject,s.topic,s.date,s.start,s.end,s.minutes,s.notes,s.rating==0?"—":s.rating});}
        refreshSubjectChoices();refreshDashboard();refreshCharts();
    }
    private void refreshSubjectChoices() {
        if(planSubject==null)return;
        loadingSubjects=true;
        String old1=(String)planSubject.getSelectedItem(),old2=(String)sessionSubject.getSelectedItem();
        planSubject.removeAllItems();sessionSubject.removeAllItems();
        for(Subject s:data.subjects){planSubject.addItem(s.name);sessionSubject.addItem(s.name);}
        if(old1!=null)planSubject.setSelectedItem(old1);if(old2!=null)sessionSubject.setSelectedItem(old2);
        loadingSubjects=false;
    }
    private void refreshDashboard() {
        if(dashboardSummary==null)return;
        int completed=0,totalMinutes=0;
        for(Plan p:data.plans)if(p.status.equals("Completed"))completed++;
        for(StudySession s:data.sessions)totalMinutes+=s.minutes;
        dashboardSummary.setText("<html><center>Study overview<br><br><font color='#4275D2'>"+data.plans.size()+"</font> plans&nbsp;&nbsp; • &nbsp;&nbsp;"+
                completed+" completed plans<br><br>"+String.format(Locale.US,"%.1f",totalMinutes/60.0)+" study hours&nbsp;&nbsp; • &nbsp;&nbsp;"+
                data.sessions.size()+" study sessions</center></html>");
        StringBuilder week=new StringBuilder("<html><center><b>This week's plans</b><br><br>");
        String today=LocalDate.now().getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL,Locale.ENGLISH);
        int count=0,done=0;
        for(Plan p:data.plans){count++;if(p.status.equals("Completed"))done++;}
        week.append("Today is ").append(today).append(".<br>");
        week.append(count).append(" plans in your timetable; ").append(done).append(" marked completed.<br>");
        week.append("Keep making steady progress!</center></html>");
        dashboardWeek.setText(week.toString());
    }
    private void refreshCharts() {
        if(statsPanel==null)return;
        statsPanel.removeAll();
        int total=0;for(StudySession s:data.sessions)total+=s.minutes;
        statsPanel.add(new BarChart("Study time by subject",subjectMinutes()));
        int completed=0;for(Plan p:data.plans)if(p.status.equals("Completed"))completed++;
        statsPanel.add(new PieChart("Plan status",new String[]{"Completed","Not completed"},new int[]{completed,data.plans.size()-completed}));
        statsPanel.add(new BarChart("Weekly study progress",weeklyMinutes()));
        statsPanel.add(new BarChart("Study sessions over time",dateMinutes()));
        statsPanel.revalidate();statsPanel.repaint();
    }
    private LinkedHashMap<String,Integer> subjectMinutes() {
        LinkedHashMap<String,Integer> m=new LinkedHashMap<>();
        for(StudySession s:data.sessions)m.put(s.subject,m.getOrDefault(s.subject,0)+s.minutes);
        return m;
    }
    private LinkedHashMap<String,Integer> weeklyMinutes() {
        LinkedHashMap<String,Integer> m=new LinkedHashMap<>();
        LocalDate now=LocalDate.now();
        LocalDate monday=now.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        for(int i=0;i<7;i++)m.put(monday.plusDays(i).format(DateTimeFormatter.ofPattern("EEE")),0);
        for(StudySession s:data.sessions)try{
            LocalDate d=LocalDate.parse(s.date);
            if(!d.isBefore(monday)&&!d.isAfter(monday.plusDays(6))){
                String key=d.format(DateTimeFormatter.ofPattern("EEE"));m.put(key,m.get(key)+s.minutes);
            }
        }catch(Exception ignored){}
        return m;
    }
    private LinkedHashMap<String,Integer> dateMinutes() {
        TreeMap<LocalDate,Integer> sorted=new TreeMap<>();
        for(StudySession s:data.sessions)try{LocalDate d=LocalDate.parse(s.date);sorted.put(d,sorted.getOrDefault(d,0)+s.minutes);}catch(Exception ignored){}
        LinkedHashMap<String,Integer> result=new LinkedHashMap<>();
        for(Map.Entry<LocalDate,Integer> e:sorted.entrySet())result.put(e.getKey().format(DateTimeFormatter.ofPattern("MM/dd")),e.getValue());
        while(result.size()>10)result.remove(result.keySet().iterator().next());
        return result;
    }

    // Custom Java2D bar chart; no external chart package is needed.
    class BarChart extends JPanel {
        String title;LinkedHashMap<String,Integer> values;
        BarChart(String t,LinkedHashMap<String,Integer> v){title=t;values=v;setBackground(Color.WHITE);setBorder(BorderFactory.createLineBorder(new Color(220,226,237)));}
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(NAVY);g2.setFont(new Font("SansSerif",Font.BOLD,14));g2.drawString(title,15,23);
            int w=getWidth(),h=getHeight(),left=38,bottom=h-35,top=42;
            g2.setColor(new Color(220,225,234));g2.drawLine(left,top,left,bottom);g2.drawLine(left,bottom,w-12,bottom);
            int max=1;for(int v:values.values())max=Math.max(max,v);
            int n=Math.max(1,values.size()),slot=Math.max(12,(w-left-25)/n),bar=Math.max(5,slot-9),i=0;
            for(Map.Entry<String,Integer> e:values.entrySet()){
                int bh=(int)((bottom-top-5)*(e.getValue()/(double)max));
                int x=left+7+i*slot;
                g2.setColor(BLUE);g2.fillRoundRect(x,bottom-bh,bar,bh,5,5);
                g2.setColor(new Color(80,90,110));g2.setFont(new Font("SansSerif",Font.PLAIN,9));
                String label=e.getKey();if(label.length()>8)label=label.substring(0,8);
                g2.drawString(label,x-2,bottom+14);i++;
            }
            g2.dispose();
        }
    }
    class PieChart extends JPanel {
        String title;String[] labels;int[] values;
        PieChart(String t,String[] l,int[] v){title=t;labels=l;values=v;setBackground(Color.WHITE);setBorder(BorderFactory.createLineBorder(new Color(220,226,237)));}
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(NAVY);g2.setFont(new Font("SansSerif",Font.BOLD,14));g2.drawString(title,15,23);
            int sum=0;for(int v:values)sum+=v;
            int size=Math.min(getWidth()-80,getHeight()-70);size=Math.max(50,size);
            int x=(getWidth()-size)/2,y=35;int start=0;
            Color[] colors={GREEN,new Color(230,166,70)};
            if(sum==0){g2.setColor(new Color(225,230,238));g2.fillOval(x,y,size,size);}
            else for(int i=0;i<values.length;i++){
                int angle=(int)Math.round(values[i]*360.0/sum);
                g2.setColor(colors[i%colors.length]);g2.fillArc(x,y,size,size,start,angle);start+=angle;
            }
            g2.setFont(new Font("SansSerif",Font.PLAIN,11));
            for(int i=0;i<labels.length;i++){g2.setColor(colors[i%colors.length]);g2.fillRect(15, getHeight()-30+i*14,10,10);
                g2.setColor(NAVY);g2.drawString(labels[i]+": "+values[i],30,getHeight()-21+i*14);}
            g2.dispose();
        }
    }

    private void loadData() {
        File f=new File(DATA_FILE);if(!f.exists())return;
        try(ObjectInputStream in=new ObjectInputStream(new FileInputStream(f))){
            Object o=in.readObject();if(o instanceof AppData)data=(AppData)o;
        }catch(Exception ex){JOptionPane.showMessageDialog(null,"Could not load saved data. A fresh workspace will be used.\n"+ex.getMessage(),"Load error",JOptionPane.WARNING_MESSAGE);}
    }
    private void saveData() {
        try(ObjectOutputStream out=new ObjectOutputStream(new FileOutputStream(DATA_FILE))){out.writeObject(data);}
        catch(IOException ex){error("Could not save data: "+ex.getMessage());}
    }
    private void message(String s){JOptionPane.showMessageDialog(this,s,"StudyTrack",JOptionPane.INFORMATION_MESSAGE);}
    private void error(String s){JOptionPane.showMessageDialog(this,s,"Please check",JOptionPane.ERROR_MESSAGE);}
    private boolean confirm(String s){return JOptionPane.showConfirmDialog(this,s,"Confirm",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION;}
    private String safeMessage(Exception ex){return ex.getMessage()==null?"Invalid value.":ex.getMessage();}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch(Exception ignored) {}
            new StudyTrack().setVisible(true);
        });
    }
}
