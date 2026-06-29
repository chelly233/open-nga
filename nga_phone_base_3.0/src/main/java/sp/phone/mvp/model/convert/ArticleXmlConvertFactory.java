package sp.phone.mvp.model.convert;

import android.text.TextUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import sp.phone.http.bean.Attachment;
import sp.phone.http.bean.ThreadData;
import sp.phone.http.bean.ThreadRowInfo;
import sp.phone.mvp.model.entity.ThreadPageInfo;
import sp.phone.util.NLog;

public class ArticleXmlConvertFactory {

    private static final String TAG = ArticleXmlConvertFactory.class.getSimpleName();

    public static ThreadData getArticleInfo(String xml) {
        try {
            if (TextUtils.isEmpty(xml)) {
                return null;
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            setFeatureIfSupported(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
            setFeatureIfSupported(factory, "http://xml.org/sax/features/external-general-entities", false);
            setFeatureIfSupported(factory, "http://xml.org/sax/features/external-parameter-entities", false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            Element root = document.getDocumentElement();
            Element topicElement = firstChild(root, "__T");
            Element rowsElement = firstChild(root, "__R");
            if (topicElement == null || rowsElement == null) {
                return null;
            }

            ThreadPageInfo threadInfo = buildThreadPageInfo(topicElement);
            NodeList rowNodes = rowsElement.getChildNodes();
            java.util.List<ThreadRowInfo> rowList = new java.util.ArrayList<>();
            for (int i = 0; i < rowNodes.getLength(); i++) {
                Node node = rowNodes.item(i);
                if (node instanceof Element && "item".equals(node.getNodeName())) {
                    ThreadRowInfo row = buildThreadRowInfo((Element) node, root);
                    if (row != null) {
                        rowList.add(row);
                    }
                }
            }
            if (rowList.isEmpty()) {
                return null;
            }

            ThreadData data = new ThreadData();
            data.setRawData(xml);
            data.setThreadInfo(threadInfo);
            data.setRowList(rowList);
            data.set__ROWS(parseInt(textOf(root, "__ROWS"), rowList.size()));
            data.setRowNum(rowList.size());
            return data;
        } catch (Exception e) {
            NLog.e(TAG, "can not parse xml: " + e.getClass().getSimpleName() + " " + e.getMessage());
            return null;
        }
    }

    private static void setFeatureIfSupported(DocumentBuilderFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (ParserConfigurationException e) {
            // Some Android XML parsers do not expose these hardening flags.
        }
    }

    private static ThreadPageInfo buildThreadPageInfo(Element element) {
        ThreadPageInfo info = new ThreadPageInfo();
        info.setTid(parseInt(textOf(element, "tid"), 0));
        info.setFid(parseInt(textOf(element, "fid"), 0));
        info.setAuthor(textOf(element, "author"));
        info.setAuthorId(parseInt(textOf(element, "authorid"), 0));
        info.setLastPoster(textOf(element, "lastposter"));
        info.setReplies(parseInt(textOf(element, "replies"), 0));
        info.setSubject(textOf(element, "subject"));
        info.setTitleFont(textOf(element, "titlefont"));
        info.setType(parseInt(textOf(element, "type"), 0));
        info.setTopicMisc(textOf(element, "topic_misc"));
        info.setPostDate(parseInt(textOf(element, "postdate"), 0));
        return info;
    }

    private static ThreadRowInfo buildThreadRowInfo(Element element, Element root) {
        ThreadRowInfo row = new ThreadRowInfo();
        row.setTid(parseInt(textOf(element, "tid"), 0));
        row.setFid(parseInt(textOf(element, "fid"), 0));
        row.setAuthorid(parseInt(textOf(element, "authorid"), 0));
        row.setSubject(textOf(element, "subject"));
        row.setPostdate(textOf(element, "postdate"));
        row.setPid(parseInt(textOf(element, "pid"), 0));
        row.setLou(parseInt(textOf(element, "lou"), 0));
        row.setContent(textOf(element, "content"));
        row.setAlterinfo(textOf(element, "alterinfo"));
        row.setScore(parseInt(textOf(element, "score"), 0));
        row.setScore_2(parseInt(textOf(element, "score_2"), 0));
        row.setFromClient(textOf(element, "from_client"));
        row.setAttachs(buildAttachments(firstChild(element, "attachs")));
        fillUserInfo(row, root);
        ArticleConvertFactory.buildRowContent(row);
        return row;
    }

    private static Map<String, Attachment> buildAttachments(Element attachsElement) {
        if (attachsElement == null) {
            return null;
        }
        Map<String, Attachment> attachs = new LinkedHashMap<>();
        NodeList nodes = attachsElement.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (!(node instanceof Element) || !"item".equals(node.getNodeName())) {
                continue;
            }
            Element item = (Element) node;
            Attachment attachment = new Attachment();
            attachment.setAttachurl(textOf(item, "attachurl"));
            attachment.setSize(parseInt(textOf(item, "size"), 0));
            attachment.setType(textOf(item, "type"));
            attachment.setSubid(parseInt(textOf(item, "subid"), 0));
            attachment.setUrl_utf8_org_name(textOf(item, "url_utf8_org_name"));
            attachment.setDscp(textOf(item, "dscp"));
            attachment.setName(textOf(item, "name"));
            attachment.setExt(textOf(item, "ext"));
            attachment.setThumb(textOf(item, "thumb"));
            attachs.put(String.valueOf(attachment.getSubid()), attachment);
        }
        return attachs.isEmpty() ? null : attachs;
    }

    private static void fillUserInfo(ThreadRowInfo row, Element root) {
        Element users = firstChild(root, "__U");
        if (users == null || row.getAuthorid() == 0) {
            return;
        }
        Element user = findUser(users, row.getAuthorid());
        if (user == null) {
            return;
        }
        row.setAuthor(textOf(user, "username"));
        row.setJs_escap_avatar(textOf(user, "avatar"));
        row.setYz(textOf(user, "yz"));
        row.setMuteTime(textOf(user, "mute_time"));
        row.setSignature(textOf(user, "signature"));
        row.setPostCount(textOf(user, "postnum"));
        row.setAurvrc(parseInt(textOf(user, "rvrc"), 0));
        row.setReputation(parseInt(textOf(user, "rvrc"), 0) / 10.0f);
    }

    private static Element findUser(Element users, int authorId) {
        NodeList nodes = users.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element && "item".equals(node.getNodeName())
                    && authorId == parseInt(textOf((Element) node, "uid"), 0)) {
                return (Element) node;
            }
        }
        return null;
    }

    private static Element firstChild(Element element, String name) {
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element && name.equals(node.getNodeName())) {
                return (Element) node;
            }
        }
        return null;
    }

    private static String textOf(Element element, String name) {
        Element child = firstChild(element, name);
        return child == null ? "" : child.getTextContent();
    }

    private static int parseInt(String value, int defaultValue) {
        try {
            return TextUtils.isEmpty(value) ? defaultValue : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
