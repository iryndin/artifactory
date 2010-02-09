package org.artifactory.webapp.wicket.security.acl;

import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * @author Yossi Shaul
 */
public abstract class BaseSortableAceInfoRowDataProvider extends SortableDataProvider {
    protected List<AceInfoRow> aces;

    public BaseSortableAceInfoRowDataProvider() {
        setSort("principal", true);
    }

    public abstract void loadData();

    @SuppressWarnings({"OverlyComplexMethod"})
    public Iterator iterator(int first, int count) {
        SortParam sp = getSort();
        String sortProp = sp.getProperty();
        boolean asc = sp.isAscending();
        //TODO: [by yl] Update the ComparablePropertySorter to support booleans and primitives
        //and use that instead
        if ("principal".equals(sortProp)) {
            if (asc) {
                Collections.sort(aces, new Comparator<AceInfoRow>() {
                    public int compare(AceInfoRow r1, AceInfoRow r2) {
                        String o1 = r1.getPrincipal();
                        String o2 = r2.getPrincipal();
                        return o1.compareTo(o2);
                    }
                });
            } else {
                Collections.sort(aces, new Comparator<AceInfoRow>() {
                    public int compare(AceInfoRow r1, AceInfoRow r2) {
                        String o1 = r1.getPrincipal();
                        String o2 = r2.getPrincipal();
                        return o2.compareTo(o1);
                    }
                });
            }
        } else if ("admin".equals(sortProp)) {
            if (asc) {
                Collections.sort(aces, new Comparator<AceInfoRow>() {
                    public int compare(AceInfoRow r1, AceInfoRow r2) {
                        boolean o1 = r1.isAdmin();
                        boolean o2 = r2.isAdmin();
                        if (o1 && o2) {
                            return 0;
                        }
                        if (!o1 && !o2) {
                            return 0;
                        }
                        if (o1 && !o2) {
                            return 1;
                        }
                        return -1;
                    }
                });
            } else {
                Collections.sort(aces, new Comparator<AceInfoRow>() {
                    public int compare(AceInfoRow r1, AceInfoRow r2) {
                        boolean o1 = r1.isAdmin();
                        boolean o2 = r2.isAdmin();
                        if (o1 && o2) {
                            return 0;
                        }
                        if (!o1 && !o2) {
                            return 0;
                        }
                        if (o2 && !o1) {
                            return 1;
                        }
                        return -1;
                    }
                });
            }
        } else if ("delete".equals(sortProp)) {
            if (asc) {
                Collections.sort(aces, new Comparator<AceInfoRow>() {
                    public int compare(AceInfoRow r1, AceInfoRow r2) {
                        boolean o1 = r1.isDelete();
                        boolean o2 = r2.isDelete();
                        if (o1 && o2) {
                            return 0;
                        }
                        if (!o1 && !o2) {
                            return 0;
                        }
                        if (o1 && !o2) {
                            return 1;
                        }
                        return -1;
                    }
                });
            } else {
                Collections.sort(aces, new Comparator<AceInfoRow>() {
                    public int compare(AceInfoRow r1, AceInfoRow r2) {
                        boolean o1 = r1.isDelete();
                        boolean o2 = r2.isDelete();
                        if (o1 && o2) {
                            return 0;
                        }
                        if (!o1 && !o2) {
                            return 0;
                        }
                        if (o2 && !o1) {
                            return 1;
                        }
                        return -1;
                    }
                });
            }
        } else if ("deploy".equals(sortProp)) {
            if (asc) {
                Collections.sort(aces, new Comparator<AceInfoRow>() {
                    public int compare(AceInfoRow r1, AceInfoRow r2) {
                        boolean o1 = r1.isDeploy();
                        boolean o2 = r2.isDeploy();
                        if (o1 && o2) {
                            return 0;
                        }
                        if (!o1 && !o2) {
                            return 0;
                        }
                        if (o1 && !o2) {
                            return 1;
                        }
                        return -1;
                    }
                });
            } else {
                Collections.sort(aces, new Comparator<AceInfoRow>() {
                    public int compare(AceInfoRow r1, AceInfoRow r2) {
                        boolean o1 = r1.isDeploy();
                        boolean o2 = r2.isDeploy();
                        if (o1 && o2) {
                            return 0;
                        }
                        if (!o1 && !o2) {
                            return 0;
                        }
                        if (o2 && !o1) {
                            return 1;
                        }
                        return -1;
                    }
                });
            }
        } else if ("read".equals(sortProp)) {
            if (asc) {
                Collections.sort(aces, new Comparator<AceInfoRow>() {
                    public int compare(AceInfoRow r1, AceInfoRow r2) {
                        boolean o1 = r1.isRead();
                        boolean o2 = r2.isRead();
                        if (o1 && o2) {
                            return 0;
                        }
                        if (!o1 && !o2) {
                            return 0;
                        }
                        if (o1 && !o2) {
                            return 1;
                        }
                        return -1;
                    }
                });
            } else {
                Collections.sort(aces, new Comparator<AceInfoRow>() {
                    public int compare(AceInfoRow r1, AceInfoRow r2) {
                        boolean o1 = r1.isRead();
                        boolean o2 = r2.isRead();
                        if (o1 && o2) {
                            return 0;
                        }
                        if (!o1 && !o2) {
                            return 0;
                        }
                        if (o2 && !o1) {
                            return 1;
                        }
                        return -1;
                    }
                });
            }
        }
        List<AceInfoRow> list = aces.subList(first, first + count);
        return list.iterator();
    }

    public int size() {
        return aces.size();
    }

    public IModel model(Object object) {
        return new Model((AceInfoRow) object);
    }
}
