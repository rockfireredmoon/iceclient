package org.icemoon.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.icelib.AbstractGameObject;
import org.icelib.Hash;

@SuppressWarnings("serial")
public class Account extends AbstractGameObject<String> {

    private final static Logger LOG = Logger.getLogger(Account.class.getName());

    public static class Build extends AbstractGameObject<Long> {

        private Rectangle bounds = new Rectangle(0, 0, 3, 3);

        public Build(Long id, Rectangle bounds) {
            super(id);
            this.bounds = bounds;
        }

        public Build() {
            this(null, new Rectangle(0, 0, 0, 0));
        }

        public final Rectangle getBounds() {
            return bounds;
        }

        public final void setBounds(Rectangle bounds) {
            this.bounds = bounds;
        }

        @Override
        public void set(String name, String value, String section) {
        }
    }

    public enum Permission {

        GM_CHAT("GmChat", "GM Chat Channel"), SYS_CHAT("SysChat", "System Chat Channel"), TWEAK_SELF("TweakSelf",
        "Tweak Own Characters"), CREATE_PROP("CreateProp", "Create props"), EDIT_PROP_SELF("EditPropSelf", "Edit own props"), EDIT_PROP_OTHER(
        "EditPropOther", "Edit other props"), EDIT_STAT("EditStat", "Edit stats"), TWEAK_OTHER("TweakOther",
        "Tweak Other Characters"), WEB_ADMIN("WebAdmin", "Allow administration via World Editor"), REGION_CHAT(
        "RegionChat", "Speak on region chat");
        private final String textName;
        private final String english;

        private Permission(String textName, String english) {
            this.textName = textName;
            this.english = english;
        }

        public String toString() {
            return english;
        }

        public static String toCommaSeparatedList(Collection<Permission> list) {
            StringBuilder bui = new StringBuilder();
            if (list != null) {
                for (Permission o : list) {
                    if (bui.length() > 0) {
                        bui.append(",");
                    }
                    bui.append(o.textName.toLowerCase());
                }
            }
            return bui.toString();
        }

        public static Permission fromTextName(String textName) {
            for (Permission n : values()) {
                if (n.textName.equalsIgnoreCase(textName)) {
                    return n;
                }
            }
            return null;
        }
    }
    private List<Permission> permissions = new ArrayList<Permission>();
    private String auth = "";
    private List<Long> characters = new ArrayList<Long>();
    private Properties prefs = new Properties();
    private List<Build> builds = new ArrayList<Build>();
    private String groveName;

    @Override
    public void set(String name, String value, String section) {
        if (name.equals("Pref")) {
            int idx = value.indexOf(",");
            prefs.setProperty(value.substring(0, idx), value.substring(idx + 1));
        } else if (name.equals("Build")) {
            Build newBuild = new Build();
            StringTokenizer t = new StringTokenizer(value, ",");
            newBuild.setEntityId(Long.parseLong(t.nextToken()));
            newBuild.setBounds(new Rectangle(t));
            builds.add(newBuild);
        } else if (name.equals("Name")) {
            setEntityId(value);
        } else if (name.equals("Auth")) {
            auth = value;
        } else if (name.equals("GroveName")) {
            groveName = value;
        } else if (name.equals("Characters")) {
            characters = new ArrayList<Long>();
            for (String id : Arrays.asList(value.split(","))) {
                if (!id.equals("")) {
                    characters.add(Long.parseLong(id));
                }
            }
        } else if (name.equals("Permissions")) {
            for (String perm : Arrays.asList(value.split(","))) {
                Permission p = Permission.fromTextName(perm);
                if (p == null) {
                    LOG.severe("Unknown permission '" + perm + "'");
                } else {
                    permissions.add(p);
                }
            }
        } else if (name.equals("WebPermissions")) {
            for (String perm : Arrays.asList(value.split(","))) {
                Permission p = Permission.fromTextName(perm);
                if (p == null) {
                    LOG.severe("Unknown web permission '" + perm + "'");
                } else {
                    if (p.equals(Permission.WEB_ADMIN)) {
                        permissions.add(p);
                    }
                }
            }
        } else if (!name.equals("")) {
            LOG.severe("Unhandled property " + name + " = " + value);
        }
    }

    public String getGroveName() {
        return groveName;
    }

    public void setGroveName(String groveName) {
        this.groveName = groveName;
    }

    public final List<Permission> getPermissions() {
        return permissions;
    }

    public final void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
    }

    public final String getAuth() {
        return auth;
    }

    public final void setAuth(String auth) {
        if (this.auth == null || !this.auth.equals(auth)) {
            try {
                this.auth = hash(auth);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public final List<Long> getCharacters() {
        return characters;
    }

    public final void setCharacters(List<Long> characters) {
        this.characters = characters;
    }

    @Override
    public String toString() {
        return getEntityId();
    }

    public final Properties getPrefs() {
        return prefs;
    }

    public final void setPrefs(Properties prefs) {
        this.prefs = prefs;
    }

    public final List<Build> getBuilds() {
        return builds;
    }

    public final void setBuilds(List<Build> builds) {
        this.builds = builds;
    }

    public boolean checkPassword(String password) {
        final String hash = hash(password);
        return this.auth.equals(hash);
    }

    public String hash(String text) {
        return Hash.hash(text, getEntityId());
    }

    public Build getBuild(long instanceId) {
        for (Build b : builds) {
            if (b.getEntityId().equals(instanceId)) {
                return b;
            }
        }
        return null;
    }
    
}
