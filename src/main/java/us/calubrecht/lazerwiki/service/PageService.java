package us.calubrecht.lazerwiki.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import us.calubrecht.lazerwiki.model.Page;
import us.calubrecht.lazerwiki.model.PageDescriptor;
import us.calubrecht.lazerwiki.repository.IdRepository;
import us.calubrecht.lazerwiki.repository.PageRepository;
import us.calubrecht.lazerwiki.service.exception.PageWriteException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional(rollbackFor = PageWriteException.class )
public class PageService {

    @Autowired
    PageRepository pageRepository;

    @Autowired
    IdRepository idRepository;

    public boolean exists(String site, String pageName) {
        PageDescriptor pageDescriptor = decodeDescriptor(pageName);
        Page p = pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(site, pageDescriptor.namespace(), pageDescriptor.pageName(), false);
        return p != null;
    }

    public String getTitle(String site, String pageName) {
        PageDescriptor pageDescriptor = decodeDescriptor(pageName);
        Page p = pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(site, pageDescriptor.namespace(), pageDescriptor.pageName(), false);
        return p == null ? Arrays.stream(pageName.split(":")).reduce((first, second) -> second)
                .orElse(null) : p.getTitle();
    }

    public String getSource(String host, String sPageDescriptor, String userName) {
        PageDescriptor pageDescriptor = decodeDescriptor(sPageDescriptor);
        Page p = pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(host, pageDescriptor.namespace(), pageDescriptor.pageName(), false);
        if (p == null ) {
            return "This page doesn't exist";
        }
        return p.getText().formatted(userName, pageDescriptor);
    }

    public PageDescriptor decodeDescriptor(String pageDescriptor) {
        List<String> tokens = new ArrayList<>(Arrays.asList(pageDescriptor.split(":")));
        String pageName = tokens.remove(tokens.size() -1);
        return new PageDescriptor(String.join(":", tokens), pageName);
    }

    public void savePage(String host, String sPageDescriptor, String text) throws PageWriteException{
        // get Existing
        PageDescriptor pageDescriptor = decodeDescriptor(sPageDescriptor);
        Page p = pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(host, pageDescriptor.namespace(), pageDescriptor.pageName(), false);
        long id = p == null ? getNewId() : p.getId();
        long revision = p == null ? 1 : p.getRevision() + 1;
        if (p != null ) {
            // invalidate old page.
            pageRepository.save(p);
        }
        Page newP = new Page();
        newP.setSite("default");
        newP.setNamespace(pageDescriptor.namespace());
        newP.setPagename(pageDescriptor.pageName());
        newP.setText(text);
        newP.setId(id);
        newP.setRevision(revision);
        pageRepository.save(newP);
    }

    protected long getNewId() {

      return idRepository.getNewId();
    }
}
