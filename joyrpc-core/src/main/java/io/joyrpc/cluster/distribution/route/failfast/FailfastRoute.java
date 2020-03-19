package io.joyrpc.cluster.distribution.route.failfast;

/*-
 * #%L
 * joyrpc
 * %%
 * Copyright (C) 2019 joyrpc.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import io.joyrpc.Result;
import io.joyrpc.cluster.Candidate;
import io.joyrpc.cluster.Node;
import io.joyrpc.cluster.distribution.Route;
import io.joyrpc.cluster.distribution.route.AbstractRoute;
import io.joyrpc.extension.Extension;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import java.util.concurrent.CompletableFuture;

import static io.joyrpc.cluster.distribution.Route.FAIL_FAST;

/**
 * 快速失败
 */
@Extension(value = FAIL_FAST, order = Route.ORDER_FAILFAST)
public class FailfastRoute extends AbstractRoute {

    @Override
    public CompletableFuture<Result> invoke(final RequestMessage<Invocation> request, final Candidate candidate) {
        Node node = loadBalance.select(candidate, request);
        return operation.apply(node, null, request);
    }

}
