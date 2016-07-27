package fi.helsinki.cs.tmc.intellij.ui.submissionresult.feedback;

import fi.helsinki.cs.tmc.core.domain.submission.FeedbackAnswer;
import fi.helsinki.cs.tmc.core.domain.submission.FeedbackQuestion;

import java.awt.Font;

import java.util.Hashtable;

import javax.swing.JLabel;
import javax.swing.JTextField;



public class IntRangeQuestionPanel extends FeedbackQuestionPanel {

    private FeedbackQuestion question;
    private int naValue;

    public IntRangeQuestionPanel(FeedbackQuestion question) {
        if (!question.isIntRange()) {
            throw new IllegalArgumentException("Invalid panel for question type");
        }

        this.question = question;

        initComponents();
        this.questionLabel.setText(question.getQuestion());
        setUpValueSlider(question);
    }

    private void setUpValueSlider(FeedbackQuestion question) {
        naValue = question.getIntRangeMin() - 1;
        valueSlider.setMinimum(naValue);
        valueSlider.setMaximum(question.getIntRangeMax());

        Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();

        JLabel naLabel = new JLabel("-");
        naLabel.setFont(naLabel.getFont().deriveFont(Font.PLAIN));
        labelTable.put(naValue, naLabel);

        for (int i = question.getIntRangeMin(); i <= question.getIntRangeMax(); ++i) {
            JLabel label = new JLabel(Integer.toString(i));
            label.setFont(naLabel.getFont().deriveFont(Font.BOLD));
            labelTable.put(i, label);
        }
        valueSlider.setLabelTable(labelTable);

        valueSlider.setValue(naValue);
    }

    @Override
    public FeedbackAnswer getAnswer() {
        int sliderValue = valueSlider.getValue();
        if (sliderValue == naValue) {
            return null;
        } else {
            return new FeedbackAnswer(question, Integer.toString(sliderValue));
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        questionLabel = new javax.swing.JLabel();
        valueSlider = new javax.swing.JSlider();

        JTextField myTextField = new JTextField("Testin popupfactory", 20);
        questionLabel.setText("Testing (intRangeQuestionPanel");
        //questionLabel.setText(org.openide.util.NbBundle.getMessage(IntRangeQuestionPanel.class, "IntRangeQuestionPanel.questionLabel.text")); // NOI18N

        valueSlider.setMaximum(10);
        valueSlider.setMinimum(1);
        valueSlider.setMinorTickSpacing(1);
        valueSlider.setPaintLabels(true);
        valueSlider.setPaintTicks(true);
        valueSlider.setSnapToTicks(true);
        valueSlider.setValue(5);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax
                                        .swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(valueSlider,
                                                javax.swing.GroupLayout
                                                        .DEFAULT_SIZE, 218, Short.MAX_VALUE)
                                        .addComponent(questionLabel))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap(javax.swing
                                        .GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(questionLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(valueSlider, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables

    private javax.swing.JLabel questionLabel;

    private javax.swing.JSlider valueSlider;

    // End of variables declaration//GEN-END:variables
}
