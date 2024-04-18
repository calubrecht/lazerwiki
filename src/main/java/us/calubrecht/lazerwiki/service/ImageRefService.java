package us.calubrecht.lazerwiki.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.model.ImageRef;
import us.calubrecht.lazerwiki.model.PageDescriptor;
import us.calubrecht.lazerwiki.repository.ImageRefRepository;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ImageRefService {
  @Autowired
  ImageRefRepository imageRefRepository;

  @Transactional
  public void setImageRefsFromPage(String site, String pageNS, String pageName, Collection<String> targets) {
    imageRefRepository.deleteBySiteAndSourcePageNSAndSourcePageName(site, pageNS, pageName);
    List<ImageRef> imageRefs = targets.stream().map(t -> {
      PageDescriptor pd = PageService.decodeDescriptor(t);
      return new ImageRef(site, pageNS, pageName, pd.namespace(), pd.pageName());
    }).collect(Collectors.toList());
    imageRefRepository.saveAll(imageRefs);
  }

  public List<String> getImagesOnPage(String site, String page) {
    PageDescriptor pd = PageService.decodeDescriptor(page);
    return imageRefRepository.findAllBySiteAndSourcePageNSAndSourcePageName(site, pd.namespace(), pd.pageName()).
            stream().map(ref -> ref.getImageNS().isBlank() ? ref.getImageRef() : ref.getImageNS() +":" + ref.getImageRef()).collect(Collectors.toList());
  }

  public List<String> getRefsForImage(String site, String imageRef) {
    PageDescriptor pd = PageService.decodeDescriptor(imageRef);
    return imageRefRepository.findAllBySiteAndImageNSAndImageRef(site, pd.namespace(), pd.pageName()).
            stream().map(ref -> ref.getSourcePageNS().isBlank() ? ref.getSourcePageName() : ref.getSourcePageNS() +":" + ref.getSourcePageName()).collect(Collectors.toList());
  }

  public void deleteImageRefs(String site, String page) {
    PageDescriptor pd = PageService.decodeDescriptor(page);
    imageRefRepository.deleteBySiteAndSourcePageNSAndSourcePageName(site, pd.namespace(), pd.pageName());

  }
}
