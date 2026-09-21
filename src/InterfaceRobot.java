import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class InterfaceRobot extends JFrame {

    private static final int LARGEUR = 8;
    private static final int HAUTEUR = 8;

    private static final int VIDE = 0;
    private static final int ROBOT = 1;
    private static final int TRACE = 2;
    private static final int DEPART = 10;
    private static final int ARRIVEE = 11;
    private static final int TELEPORTEUR = 66;
    private static final int MUR = 99;

    private static final int X_DEPART = 0;
    private static final int Y_DEPART = 0;
    private static final int X_ARRIVEE = 7;
    private static final int Y_ARRIVEE = 7;

    /*
     * Les dix sequences sont volontairement de longueurs differentes.
     * Certaines contiennent des ordres impossibles : sortie de la halle
     * ou collision avec un mur. Ces ordres doivent etre ignores par la
     * methode de l'apprenti.
     *
     * Toutes finissent cependant par mener le robot a la destination si
     * la methode est correcte.
     */
    private static final String[] SEQUENCES = {
            // "ESSSEEEESSSSSS",
            // "NESSSEEEESSSSSS",
            // "OESSSEEEESSSSSS",
            // "EESSSEEEESSSSSS",
            // "ESSESSEEEESSSSSS",
            // "ESSSOEEEESSSSSS",
            // "ESSSEEEEEESSSSSS",
            "NEESSSEEEEEESSSSSS",
            "OEESESSEEEEEESSSSSS",
            "NOEESSESSEEEEEESSSSSS",
            "SSSSEEESEEESES",
            "ESSSSSSSEESESE",
            "SSSSEEESEEEEONESEE",
            "SSESEENESENSSSNOSESSSS",
            "ESNSSNSOSSSEOSSEEEEEEE",
            "SESSEONESSESNSESEOSEEE"
    };

    private final Random random = new Random();

    private int[][] halleDeStockage;
    private int xRobot;
    private int yRobot;
    private String sequenceCourante;
    private int indexDirection;
    private boolean livraisonTerminee;
    private int[][] numeroIteration;

    private final HallePanel panneauHalle = new HallePanel();
    private final JList<String> listeDirections = new JList<>();
    private final JLabel etat = new JLabel(" ", SwingConstants.CENTER);
    private final JButton boutonRecommencer = new JButton("Recommencer");
    private final JButton boutonAvancer = new JButton("Avancer robot");

    public InterfaceRobot() {
        super("Robot de livraison en Java - Paul Friedli v1.0.1");

        construireInterface();
        recommencer();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setMinimumSize(new Dimension(900, 650));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void construireInterface() {
        setLayout(new BorderLayout(12, 12));

        panneauHalle.setPreferredSize(new Dimension(620, 620));
        panneauHalle.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(panneauHalle, BorderLayout.CENTER);

        JPanel panneauDroite = new JPanel(new BorderLayout(8, 8));
        panneauDroite.setPreferredSize(new Dimension(220, 620));
        panneauDroite.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 10));

        JLabel titre = new JLabel("Directions restantes", SwingConstants.CENTER);
        titre.setFont(titre.getFont().deriveFont(Font.BOLD, 16f));
        panneauDroite.add(titre, BorderLayout.NORTH);

        listeDirections.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listeDirections.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 18));
        listeDirections.setFocusable(false);
        panneauDroite.add(new JScrollPane(listeDirections), BorderLayout.CENTER);

        JPanel boutons = new JPanel(new GridLayout(3, 1, 6, 6));
        boutons.add(boutonRecommencer);
        boutons.add(boutonAvancer);
        boutons.add(etat);
        panneauDroite.add(boutons, BorderLayout.SOUTH);

        add(panneauDroite, BorderLayout.EAST);

        boutonRecommencer.addActionListener(e -> recommencer());
        boutonAvancer.addActionListener(e -> avancerRobot());
    }

    private void recommencer() {
        halleDeStockage = creerHalleInitiale();
        numeroIteration = new int[HAUTEUR][LARGEUR];
        for (int y = 0; y < HAUTEUR; y++) {
            Arrays.fill(numeroIteration[y], -1);
        }
        numeroIteration[Y_DEPART][X_DEPART] = 0;

        xRobot = X_DEPART;
        yRobot = Y_DEPART;
        halleDeStockage[yRobot][xRobot] = ROBOT;

        sequenceCourante = SEQUENCES[random.nextInt(SEQUENCES.length)];
        indexDirection = 0;
        livraisonTerminee = false;

        etat.setText("Nouvelle mission");
        boutonAvancer.setEnabled(true);
        rafraichir();
    }

    private int[][] creerHalleInitiale() {
        int[][] h = new int[HAUTEUR][LARGEUR];

        h[Y_DEPART][X_DEPART] = DEPART;
        h[Y_ARRIVEE][X_ARRIVEE] = ARRIVEE;

        // Mur vertical pres du depart.
        h[0][2] = MUR;
        h[1][2] = MUR;
        h[2][2] = MUR;

        // Quelques autres obstacles.
        h[3][4] = MUR;
        h[4][4] = MUR;
        h[4][5] = MUR;
        h[6][1] = MUR;
        h[6][2] = MUR;
        h[6][3] = MUR;

        // Teleporteur principal : (1,3) -> (3,1).
        h[3][1] = TELEPORTEUR;

        // Un second teleporteur, non utilise par toutes les sequences :
        // (6,4) -> (4,6).
        h[4][6] = TELEPORTEUR;

        return h;
    }

    private void avancerRobot() {
        if (livraisonTerminee) {
            return;
        }

        if (indexDirection >= sequenceCourante.length()) {
            boutonAvancer.setEnabled(false);
            etat.setText("Plus aucune direction");
            JOptionPane.showMessageDialog(
                    this,
                    "La sequence est terminee mais la livraison n'a pas ete effectuee.\n"
                            + "La methode deplacerRobot contient probablement une erreur.",
                    "Mission non terminee",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        char direction = sequenceCourante.charAt(indexDirection);

        int[][] avant = copier(hallDeStockageSansFaute());
        int ancienX = xRobot;
        int ancienY = yRobot;

        int retour;
        try {
            retour = RobotApprenti.deplacerRobot(
                    halleDeStockage,
                    direction,
                    xRobot,
                    yRobot);
        } catch (Exception ex) {
            restaurerApresErreur(avant, ancienX, ancienY);
            afficherErreur("Votre methode a provoque une exception :\n"
                    + ex.getClass().getSimpleName() + " : " + ex.getMessage());
            return;
        }

        ResultatAttendu attendu = calculerResultatAttendu(avant, direction, ancienX, ancienY);

        if (retour != attendu.codeRetour) {
            restaurerApresErreur(avant, ancienX, ancienY);
            afficherErreur("Valeur de retour incoherente.\n\n"
                    + "Direction demandee : " + nomDirection(direction) + "\n"
                    + "Retour attendu : " + attendu.codeRetour + "\n"
                    + "Retour obtenu : " + retour);
            return;
        }

        if (!Arrays.deepEquals(halleDeStockage, attendu.halleApres)) {
            restaurerApresErreur(avant, ancienX, ancienY);
            afficherErreur("Le tableau halleDeStockage n'a pas ete mis a jour correctement.\n\n"
                    + "La valeur de retour est correcte (" + retour
                    + "), mais le contenu du tableau ne correspond pas au deplacement attendu.");
            return;
        }

        int numeroOrdre = indexDirection + 1;
        enregistrerIteration(direction, retour, ancienX, ancienY, attendu.nouveauX, attendu.nouveauY, numeroOrdre);

        xRobot = attendu.nouveauX;
        yRobot = attendu.nouveauY;
        indexDirection++;

        if (retour == -1) {
            livraisonTerminee = true;
            boutonAvancer.setEnabled(false);
            etat.setText("Livraison effectuee !");
        } else if (retour == 0) {
            etat.setText("Ordre ignore : " + nomDirection(direction));
        } else if (retour == 5) {
            etat.setText("Teleportation !");
        } else {
            etat.setText("Deplacement : " + nomDirection(direction));
        }

        rafraichir();

        if (livraisonTerminee) {
            JOptionPane.showMessageDialog(
                    this,
                    "Bravo ! Le robot a livre son colis.",
                    "Mission accomplie",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /* Permet de garder la ligne de sauvegarde lisible dans avancerRobot(). */
    private int[][] hallDeStockageSansFaute() {
        return halleDeStockage;
    }

    private void restaurerApresErreur(int[][] avant, int ancienX, int ancienY) {
        halleDeStockage = copier(avant);
        xRobot = ancienX;
        yRobot = ancienY;
        rafraichir();
    }

    private void afficherErreur(String message) {
        etat.setText("Erreur dans deplacerRobot");
        JOptionPane.showMessageDialog(
                this,
                message,
                "Resultat incoherent",
                JOptionPane.ERROR_MESSAGE);
    }

    private ResultatAttendu calculerResultatAttendu(int[][] avant, char direction, int x, int y) {
        int[][] apres = copier(avant);

        int dx = 0;
        int dy = 0;
        int codeMouvement = 0;

        switch (direction) {
            case 'E' -> {
                dx = 1;
                codeMouvement = 1;
            }
            case 'N' -> {
                dy = -1;
                codeMouvement = 2;
            }
            case 'O' -> {
                dx = -1;
                codeMouvement = 3;
            }
            case 'S' -> {
                dy = 1;
                codeMouvement = 4;
            }
            default -> {
                return new ResultatAttendu(0, x, y, apres);
            }
        }

        int prochainX = x + dx;
        int prochainY = y + dy;

        if (prochainX < 0 || prochainX >= LARGEUR || prochainY < 0 || prochainY >= HAUTEUR) {
            return new ResultatAttendu(0, x, y, apres);
        }

        if (avant[prochainY][prochainX] == MUR) {
            return new ResultatAttendu(0, x, y, apres);
        }

        quitterAncienneCase(apres, x, y);

        if (avant[prochainY][prochainX] == TELEPORTEUR) {
            int teleporteX = prochainY;
            int teleporteY = prochainX;

            // Les teleporteurs de cette halle ont toujours une destination valide.
            apres[teleporteY][teleporteX] = ROBOT;
            return new ResultatAttendu(5, teleporteX, teleporteY, apres);
        }

        apres[prochainY][prochainX] = ROBOT;

        if (prochainX == X_ARRIVEE && prochainY == Y_ARRIVEE) {
            return new ResultatAttendu(-1, prochainX, prochainY, apres);
        }

        return new ResultatAttendu(codeMouvement, prochainX, prochainY, apres);
    }

    private void quitterAncienneCase(int[][] halle, int x, int y) {
        // if (x == X_DEPART && y == Y_DEPART) {
        // halle[y][x] = DEPART;
        // } else {
        halle[y][x] = TRACE;
        // }
    }

    private void enregistrerIteration(char direction, int retour, int ancienX, int ancienY,
            int nouveauX, int nouveauY, int numeroOrdre) {
        if (retour == 0) {
            return;
        }

        if (retour == 5) {
            int entreeX = ancienX;
            int entreeY = ancienY;

            switch (direction) {
                case 'E' -> entreeX++;
                case 'N' -> entreeY--;
                case 'O' -> entreeX--;
                case 'S' -> entreeY++;
            }

            numeroIteration[entreeY][entreeX] = numeroOrdre;
        }

        numeroIteration[nouveauY][nouveauX] = numeroOrdre;
    }

    private void rafraichir() {
        List<String> directions = new ArrayList<>();

        for (int i = indexDirection; i < sequenceCourante.length(); i++) {
            char d = sequenceCourante.charAt(i);
            String prefixe = i == indexDirection ? "-> " : "   ";
            directions.add(prefixe + nomDirection(d));
        }

        if (directions.isEmpty()) {
            directions.add("(aucune)");
        }

        listeDirections.setListData(directions.toArray(String[]::new));
        if (!directions.isEmpty()) {
            listeDirections.setSelectedIndex(0);
        }

        panneauHalle.repaint();
    }

    private String nomDirection(char direction) {
        return switch (direction) {
            case 'E' -> "EST";
            case 'N' -> "NORD";
            case 'O' -> "OUEST";
            case 'S' -> "SUD";
            default -> "?";
        };
    }

    private int[][] copier(int[][] source) {
        int[][] copie = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            copie[i] = Arrays.copyOf(source[i], source[i].length);
        }
        return copie;
    }

    private record ResultatAttendu(int codeRetour, int nouveauX, int nouveauY, int[][] halleApres) {
    }

    private class HallePanel extends JPanel {
        private final Color couleurVide = new Color(245, 245, 245);
        private final Color couleurTrace = new Color(198, 225, 255);
        private final Color couleurDepart = new Color(192, 240, 192);
        private final Color couleurArrivee = new Color(255, 220, 145);
        private final Color couleurMur = new Color(75, 75, 75);
        private final Color couleurTeleporteur = new Color(190, 145, 235);
        private final Color couleurRobot = new Color(45, 100, 210);

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (halleDeStockage == null) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int marge = 35;
            int largeurDisponible = getWidth() - 2 * marge;
            int hauteurDisponible = getHeight() - 2 * marge;
            int tailleCase = Math.min(largeurDisponible / LARGEUR, hauteurDisponible / HAUTEUR);

            int grilleLargeur = tailleCase * LARGEUR;
            int grilleHauteur = tailleCase * HAUTEUR;
            int origineX = (getWidth() - grilleLargeur) / 2;
            int origineY = (getHeight() - grilleHauteur) / 2;

            g2.setFont(getFont().deriveFont(Font.BOLD, Math.max(12f, tailleCase * 0.22f)));

            for (int y = 0; y < HAUTEUR; y++) {
                for (int x = 0; x < LARGEUR; x++) {
                    int valeur = halleDeStockage[y][x];
                    int px = origineX + x * tailleCase;
                    int py = origineY + y * tailleCase;

                    g2.setColor(couleurPour(valeur));
                    g2.fillRect(px, py, tailleCase, tailleCase);

                    g2.setColor(new Color(170, 170, 170));
                    g2.drawRect(px, py, tailleCase, tailleCase);

                    String texte = textePour(valeur);
                    if (!texte.isEmpty()) {
                        dessinerTexteCentre(g2, texte, px, py, tailleCase,
                                valeur == MUR ? Color.WHITE : Color.DARK_GRAY);
                    }

                    int iteration = numeroIteration[y][x];
                    if (iteration >= 0) {
                        dessinerNumeroIteration(g2, iteration, px, py, tailleCase, valeur);
                    }
                }
            }

            dessinerCoordonnees(g2, origineX, origineY, tailleCase);
            dessinerRobot(g2, origineX, origineY, tailleCase);

            g2.dispose();
        }

        private Color couleurPour(int valeur) {
            return switch (valeur) {
                case TRACE -> couleurTrace;
                case DEPART -> couleurDepart;
                case ARRIVEE -> couleurArrivee;
                case TELEPORTEUR -> couleurTeleporteur;
                case MUR -> couleurMur;
                default -> couleurVide;
            };
        }

        private String textePour(int valeur) {
            return switch (valeur) {
                case DEPART -> "DEPART";
                case ARRIVEE -> "ARRIVEE";
                case TELEPORTEUR -> "TP";
                case MUR -> "MUR";
                default -> "";
            };
        }

        private void dessinerNumeroIteration(Graphics2D g2, int iteration, int px, int py,
                int tailleCase, int valeur) {
            String texte = Integer.toString(iteration);
            int diametre = Math.max(22, tailleCase / 3);
            int x = px + 5;
            int y = py + 5;

            Color fond = valeur == TELEPORTEUR ? new Color(255, 255, 255, 225) : new Color(255, 255, 255, 215);
            g2.setColor(fond);
            g2.fillOval(x, y, diametre, diametre);
            g2.setColor(new Color(55, 55, 55));
            g2.drawOval(x, y, diametre, diametre);

            g2.setFont(getFont().deriveFont(Font.BOLD, Math.max(11f, tailleCase * 0.18f)));
            int largeur = g2.getFontMetrics().stringWidth(texte);
            int hauteur = g2.getFontMetrics().getAscent();
            g2.drawString(texte, x + (diametre - largeur) / 2, y + (diametre + hauteur) / 2 - 2);
        }

        private void dessinerCoordonnees(Graphics2D g2, int origineX, int origineY, int tailleCase) {
            g2.setColor(Color.DARK_GRAY);
            g2.setFont(getFont().deriveFont(Font.PLAIN, 12f));

            for (int x = 0; x < LARGEUR; x++) {
                String s = "x=" + x;
                int largeur = g2.getFontMetrics().stringWidth(s);
                g2.drawString(s, origineX + x * tailleCase + (tailleCase - largeur) / 2, origineY - 8);
            }

            for (int y = 0; y < HAUTEUR; y++) {
                String s = "y=" + y;
                int largeur = g2.getFontMetrics().stringWidth(s);
                g2.drawString(s, origineX - largeur - 8, origineY + y * tailleCase + tailleCase / 2 + 5);
            }
        }

        private void dessinerRobot(Graphics2D g2, int origineX, int origineY, int tailleCase) {
            int px = origineX + xRobot * tailleCase;
            int py = origineY + yRobot * tailleCase;
            int margeRobot = Math.max(8, tailleCase / 6);
            int diametre = tailleCase - 2 * margeRobot;

            g2.setColor(couleurRobot);
            g2.fillOval(px + margeRobot, py + margeRobot, diametre, diametre);

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(px + margeRobot, py + margeRobot, diametre, diametre);

            String texte = "R";
            g2.setFont(getFont().deriveFont(Font.BOLD, Math.max(16f, tailleCase * 0.30f)));
            dessinerTexteCentre(g2, texte, px, py, tailleCase, Color.WHITE);
        }

        private void dessinerTexteCentre(Graphics2D g2, String texte, int x, int y, int taille, Color couleur) {
            g2.setColor(couleur);
            int largeur = g2.getFontMetrics().stringWidth(texte);
            int hauteur = g2.getFontMetrics().getAscent();
            g2.drawString(texte, x + (taille - largeur) / 2, y + (taille + hauteur) / 2 - 3);
        }
    }
}
