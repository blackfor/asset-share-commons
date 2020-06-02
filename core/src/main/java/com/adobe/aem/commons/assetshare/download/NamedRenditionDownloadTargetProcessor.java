package com.adobe.aem.commons.assetshare.download;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.mime.MimeTypeService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.aem.commons.assetshare.content.AssetModel;
import com.adobe.aem.commons.assetshare.content.renditions.AssetRenditionDispatcher;
import com.adobe.aem.commons.assetshare.content.renditions.AssetRenditionDispatchers;
import com.adobe.aem.commons.assetshare.content.renditions.AssetRenditionParameters;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.download.api.DownloadApiFactory;
import com.day.cq.dam.download.api.DownloadException;
import com.day.cq.dam.download.api.DownloadFile;
import com.day.cq.dam.download.api.DownloadTarget;
import com.day.cq.dam.download.api.DownloadTargetProcessor;

import acscommons.com.google.common.util.concurrent.Service;

@Component(service = DownloadTargetProcessor.class)
public class NamedRenditionDownloadTargetProcessor implements DownloadTargetProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(NamedRenditionDownloadTargetProcessor.class);
    private static final String PARAM_PATH = "path";
    private static final String PARAM_RENDITIONNAME = "renditionName";
    
    @Reference
    private transient AssetRenditionDispatchers assetRenditionDispatchers;
    
    @Reference
    private DownloadApiFactory apiFactory;
    
    @Reference
    private MimeTypeService mimeService;
    
    @Override
    public Collection<DownloadFile> processTarget(DownloadTarget target, ResourceResolver resourceResolver) throws DownloadException {
        List<DownloadFile> answer = new ArrayList<>();
        
        String path = target.getParameter(PARAM_PATH, String.class);
        String renditionName = target.getParameter(PARAM_RENDITIONNAME, String.class);
        
        Resource resource = resourceResolver.getResource(path);
        Asset asset = resource.adaptTo(Asset.class);
        
        AssetModel model = resource.adaptTo(AssetModel.class);
        AssetRenditionParameters params = new AssetRenditionParameters(model, renditionName);

        for (final AssetRenditionDispatcher assetRenditionDispatcher : assetRenditionDispatchers.getAssetRenditionDispatchers()) {
            
            AssetRenditionDispatcher.AssetRendition renditionDetails = assetRenditionDispatcher.getRendition(asset, params);
            if(renditionDetails!=null) {
                Map<String, Object> fileParams = new HashMap<String, Object>();
                   
                fileParams.put("archivePath", getArchiveFileName(asset, renditionName, renditionDetails.getMimeType()));
                
                answer.add(apiFactory.createDownloadFile(renditionDetails.getSize(), renditionDetails.getBinaryUri(), fileParams));
            }
        }
        
        return answer;
    }
    
    private String getArchiveFileName(Asset asset, String renditionName, String mimeType) {
        return asset.getName()+"-"+renditionName+"."+mimeService.getExtension(mimeType);
    }

    @Override
    public String getTargetType() {
        return "namedrendition";
    }

    @Override
    public Map<String, Boolean> getValidParameters() {
        Map<String, Boolean> answer = new HashMap<String, Boolean>();
        answer.put(PARAM_PATH, true);
        answer.put(PARAM_RENDITIONNAME, true);
        return answer;
    }

}
