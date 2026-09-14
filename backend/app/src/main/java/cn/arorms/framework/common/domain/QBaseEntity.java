package cn.arorms.framework.common.domain;

import com.querydsl.core.types.Path;
import static com.querydsl.core.types.PathMetadataFactory.*;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.NumberPath;

import java.time.LocalDateTime;

/**
 * QueryDSL query type for the BaseEntity class supplied by arorms-common.
 * It is generated locally because the published artifact does not include Q types.
 */
public class QBaseEntity extends EntityPathBase<BaseEntity> {
    private static final long serialVersionUID = 1L;
    public static final QBaseEntity baseEntity = new QBaseEntity("baseEntity");

    public final DateTimePath<LocalDateTime> createdAt = createDateTime("createdAt", LocalDateTime.class);
    public final NumberPath<Long> id = createNumber("id", Long.class);
    public final DateTimePath<LocalDateTime> updatedAt = createDateTime("updatedAt", LocalDateTime.class);

    public QBaseEntity(String variable) {
        super(BaseEntity.class, forVariable(variable));
    }

    public QBaseEntity(Path<? extends BaseEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBaseEntity(PathMetadata metadata) {
        super(BaseEntity.class, metadata);
    }
}
