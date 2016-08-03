package it.reply.orchestrator.dto.cmdb;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Image extends CmdbDataWrapper<Image, ImageData> implements Serializable {

  private static final long serialVersionUID = -6026989231866013667L;

}